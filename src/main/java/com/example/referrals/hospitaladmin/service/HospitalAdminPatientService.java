package com.example.referrals.hospitaladmin.service;

import com.example.referrals.hospital.Hospital;
import com.example.referrals.hospital.HospitalRepository;
import com.example.referrals.hospitaladmin.form.PatientForm;
import com.example.referrals.patient.Patient;
import com.example.referrals.patient.PatientRepository;
import com.example.referrals.referral.ReferralRepository;
import com.example.referrals.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Transactional
public class HospitalAdminPatientService {

    private static final DateTimeFormatter PATIENT_NUMBER_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final PatientRepository patientRepository;
    private final HospitalRepository hospitalRepository;
    private final ReferralRepository referralRepository;

    public HospitalAdminPatientService(PatientRepository patientRepository,
                                       HospitalRepository hospitalRepository,
                                       ReferralRepository referralRepository) {
        this.patientRepository = patientRepository;
        this.hospitalRepository = hospitalRepository;
        this.referralRepository = referralRepository;
    }

    @Transactional(readOnly = true)
    public List<Patient> findAllForCurrentTenant() {
        return patientRepository.findAllByHospitalIdOrderByLastNameAscFirstNameAsc(requireTenantId());
    }

    @Transactional(readOnly = true)
    public List<Patient> findAllForCurrentTenant(String query) {
        String normalizedQuery = normalizeQuery(query);
        if (normalizedQuery == null) {
            return findAllForCurrentTenant();
        }
        return patientRepository.findAllByHospitalIdOrderByLastNameAscFirstNameAsc(requireTenantId()).stream()
            .filter(patient -> matchesPatient(patient, normalizedQuery))
            .toList();
    }

    @Transactional(readOnly = true)
    public PatientForm buildForm(Long patientId) {
        Patient patient = patientRepository.findByIdAndHospitalId(patientId, requireTenantId())
            .orElseThrow(() -> new EntityNotFoundException("Patient not found"));
        PatientForm form = new PatientForm();
        form.setPatientNumber(patient.getPatientNumber());
        form.setFirstName(patient.getFirstName());
        form.setLastName(patient.getLastName());
        form.setGender(patient.getGender());
        form.setDateOfBirth(patient.getDateOfBirth());
        form.setPhoneNumber(patient.getPhoneNumber());
        form.setNationalId(patient.getNationalId());
        return form;
    }

    @Transactional(readOnly = true)
    public Patient findByIdForCurrentTenant(Long patientId) {
        return patientRepository.findByIdAndHospitalId(patientId, requireTenantId())
            .orElseThrow(() -> new EntityNotFoundException("Patient not found"));
    }

    public void create(PatientForm form) {
        Long tenantId = requireTenantId();

        Hospital hospital = hospitalRepository.findById(tenantId)
            .orElseThrow(() -> new EntityNotFoundException("Hospital not found"));

        Patient patient = new Patient();
        patient.setHospital(hospital);
        applyForCreate(form, patient, hospital);
        patientRepository.save(patient);
    }

    public void update(Long patientId, PatientForm form) {
        Long tenantId = requireTenantId();

        Patient patient = patientRepository.findByIdAndHospitalId(patientId, tenantId)
            .orElseThrow(() -> new EntityNotFoundException("Patient not found"));
        applyForUpdate(form, patient);
    }

    public void delete(Long patientId) {
        Patient patient = patientRepository.findByIdAndHospitalId(patientId, requireTenantId())
            .orElseThrow(() -> new EntityNotFoundException("Patient not found"));
        if (referralRepository.existsByPatientId(patientId)) {
            throw new IllegalArgumentException("This patient already has referrals and cannot be deleted.");
        }
        patientRepository.delete(patient);
    }

    @Transactional(readOnly = true)
    public long countForCurrentTenant() {
        return patientRepository.countByHospitalId(requireTenantId());
    }

    private void applyForCreate(PatientForm form, Patient patient, Hospital hospital) {
        patient.setPatientNumber(generatePatientNumber(hospital));
        applyCommonFields(form, patient);
    }

    private void applyForUpdate(PatientForm form, Patient patient) {
        applyCommonFields(form, patient);
    }

    private void applyCommonFields(PatientForm form, Patient patient) {
        patient.setFirstName(form.getFirstName().trim());
        patient.setLastName(form.getLastName().trim());
        patient.setGender(form.getGender());
        patient.setDateOfBirth(form.getDateOfBirth());
        patient.setPhoneNumber(normalizeNullable(form.getPhoneNumber()));
        patient.setNationalId(normalizeNullable(form.getNationalId()));
    }

    private String normalizeNullable(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private Long requireTenantId() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant context not available");
        }
        return tenantId;
    }

    private boolean matchesPatient(Patient patient, String query) {
        return contains(patient.getPatientNumber(), query)
            || contains(patient.getFirstName(), query)
            || contains(patient.getLastName(), query)
            || contains(patient.getFullName(), query)
            || contains(patient.getPhoneNumber(), query)
            || contains(patient.getNationalId(), query);
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

    private String generatePatientNumber(Hospital hospital) {
        String hospitalCode = sanitizeHospitalCode(hospital.getCode());
        for (int attempt = 0; attempt < 20; attempt++) {
            String candidate = "PAT-" + hospitalCode + "-"
                + LocalDateTime.now().format(PATIENT_NUMBER_TIMESTAMP)
                + "-"
                + ThreadLocalRandom.current().nextInt(100, 1000);
            if (!patientRepository.existsByPatientNumberIgnoreCaseAndHospitalId(candidate, hospital.getId())) {
                return candidate;
            }
        }
        throw new IllegalStateException("Unable to generate a unique patient number right now. Please try again.");
    }

    private String sanitizeHospitalCode(String hospitalCode) {
        String normalized = hospitalCode == null ? "HOSPITAL" : hospitalCode.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
        if (!StringUtils.hasText(normalized)) {
            normalized = "HOSPITAL";
        }
        return normalized.length() > 8 ? normalized.substring(0, 8) : normalized;
    }
}
