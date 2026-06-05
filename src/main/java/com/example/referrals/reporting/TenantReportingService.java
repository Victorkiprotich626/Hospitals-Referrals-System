package com.example.referrals.reporting;

import com.example.referrals.directory.DepartmentRepository;
import com.example.referrals.directory.DoctorRepository;
import com.example.referrals.patient.PatientRepository;
import com.example.referrals.referral.Referral;
import com.example.referrals.referral.ReferralPriority;
import com.example.referrals.referral.ReferralRepository;
import com.example.referrals.referral.ReferralStatus;
import com.example.referrals.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class TenantReportingService {

    private final ReferralRepository referralRepository;
    private final PatientRepository patientRepository;
    private final DepartmentRepository departmentRepository;
    private final DoctorRepository doctorRepository;

    public TenantReportingService(ReferralRepository referralRepository,
                                  PatientRepository patientRepository,
                                  DepartmentRepository departmentRepository,
                                  DoctorRepository doctorRepository) {
        this.referralRepository = referralRepository;
        this.patientRepository = patientRepository;
        this.departmentRepository = departmentRepository;
        this.doctorRepository = doctorRepository;
    }

    public TenantReport buildCurrentTenantReport() {
        return buildCurrentTenantReport(null, null);
    }

    public TenantReport buildCurrentTenantReport(String query, ReferralStatus status) {
        Long hospitalId = requireTenantId();
        List<Referral> referrals = filterReferrals(referralRepository.findAllVisibleToHospital(hospitalId), hospitalId, query, status);

        long totalReferrals = referrals.size();
        long openReferrals = referrals.stream()
            .filter(referral -> !isClosed(referral.getStatus()))
            .count();
        long incomingReferrals = referrals.stream()
            .filter(referral -> referral.isIncomingFor(hospitalId))
            .count();
        long outgoingReferrals = referrals.stream()
            .filter(referral -> referral.isOutgoingFor(hospitalId))
            .count();
        long assignedReferrals = referrals.stream()
            .filter(referral -> referral.getAssignedDoctor() != null || referral.getAssignedDepartment() != null)
            .count();

        Map<ReferralStatus, Long> statusCounts = referrals.stream()
            .collect(Collectors.groupingBy(Referral::getStatus, LinkedHashMap::new, Collectors.counting()));
        Map<ReferralPriority, Long> priorityCounts = referrals.stream()
            .collect(Collectors.groupingBy(Referral::getPriority, LinkedHashMap::new, Collectors.counting()));
        Map<String, Long> counterpartyCounts = referrals.stream()
            .collect(Collectors.groupingBy(
                referral -> referral.isIncomingFor(hospitalId)
                    ? referral.getFromHospital().getName()
                    : referral.getToHospital().getName(),
                LinkedHashMap::new,
                Collectors.counting()
            ));

        List<Referral> recentReferrals = referrals.stream()
            .sorted(Comparator.comparing(Referral::getUpdatedAt).reversed())
            .limit(12)
            .toList();

        return new TenantReport(
            totalReferrals,
            openReferrals,
            incomingReferrals,
            outgoingReferrals,
            assignedReferrals,
            patientRepository.countByHospitalId(hospitalId),
            departmentRepository.countByHospitalIdAndEnabledTrue(hospitalId),
            doctorRepository.countByHospitalIdAndEnabledTrue(hospitalId),
            statusCounts,
            priorityCounts,
            counterpartyCounts,
            recentReferrals
        );
    }

    public String buildCsvForCurrentTenant() {
        return buildCsvForCurrentTenant(null, null);
    }

    public String buildCsvForCurrentTenant(String query, ReferralStatus status) {
        Long hospitalId = requireTenantId();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        StringBuilder csv = new StringBuilder();
        csv.append("Reference,Patient Number,Patient Name,Direction,Counterparty,Status,Priority,Assigned Department,Assigned Doctor,Updated At")
            .append(System.lineSeparator());

        for (Referral referral : filterReferrals(referralRepository.findAllVisibleToHospital(hospitalId), hospitalId, query, status)) {
            String direction = referral.isIncomingFor(hospitalId) ? "Incoming" : "Outgoing";
            String counterparty = referral.isIncomingFor(hospitalId)
                ? referral.getFromHospital().getName()
                : referral.getToHospital().getName();
            String assignedDepartment = referral.getAssignedDepartment() != null ? referral.getAssignedDepartment().getName() : "";
            String assignedDoctor = referral.getAssignedDoctor() != null ? referral.getAssignedDoctor().getFullName() : "";

            csv.append(escape(referral.getReferenceNumber())).append(',')
                .append(escape(referral.getPatient().getPatientNumber())).append(',')
                .append(escape(referral.getPatient().getFullName())).append(',')
                .append(escape(direction)).append(',')
                .append(escape(counterparty)).append(',')
                .append(escape(referral.getStatus().getDisplayName())).append(',')
                .append(escape(referral.getPriority().getDisplayName())).append(',')
                .append(escape(assignedDepartment)).append(',')
                .append(escape(assignedDoctor)).append(',')
                .append(escape(referral.getUpdatedAt().format(formatter)))
                .append(System.lineSeparator());
        }

        return csv.toString();
    }

    private boolean isClosed(ReferralStatus status) {
        return status == ReferralStatus.REJECTED
            || status == ReferralStatus.COMPLETED
            || status == ReferralStatus.CANCELLED;
    }

    private List<Referral> filterReferrals(List<Referral> referrals, Long hospitalId, String query, ReferralStatus status) {
        String normalizedQuery = normalizeQuery(query);
        return referrals.stream()
            .filter(referral -> status == null || referral.getStatus() == status)
            .filter(referral -> normalizedQuery == null || matchesReferral(referral, hospitalId, normalizedQuery))
            .toList();
    }

    private String escape(String value) {
        String safeValue = value == null ? "" : value.replace("\"", "\"\"");
        return "\"" + safeValue + "\"";
    }

    private Long requireTenantId() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant context not available");
        }
        return tenantId;
    }

    private boolean matchesReferral(Referral referral, Long hospitalId, String query) {
        return contains(referral.getReferenceNumber(), query)
            || contains(referral.getJourneyCode(), query)
            || contains(referral.getSubject(), query)
            || contains(referral.getPatient().getPatientNumber(), query)
            || contains(referral.getPatient().getFullName(), query)
            || contains(referral.getStatus().getDisplayName(), query)
            || contains(referral.getPriority().getDisplayName(), query)
            || contains(referral.isIncomingFor(hospitalId) ? referral.getFromHospital().getName() : referral.getToHospital().getName(), query)
            || (referral.getAssignedDepartment() != null && contains(referral.getAssignedDepartment().getName(), query))
            || (referral.getAssignedDoctor() != null && contains(referral.getAssignedDoctor().getFullName(), query));
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }

    private String normalizeQuery(String query) {
        if (!StringUtils.hasText(query)) {
            return null;
        }
        return query.trim().toLowerCase(Locale.ROOT);
    }

    public record TenantReport(long totalReferrals,
                               long openReferrals,
                               long incomingReferrals,
                               long outgoingReferrals,
                               long assignedReferrals,
                               long patientCount,
                               long departmentCount,
                               long doctorCount,
                               Map<ReferralStatus, Long> statusCounts,
                               Map<ReferralPriority, Long> priorityCounts,
                               Map<String, Long> counterpartyCounts,
                               List<Referral> recentReferrals) {
    }
}
