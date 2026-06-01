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
