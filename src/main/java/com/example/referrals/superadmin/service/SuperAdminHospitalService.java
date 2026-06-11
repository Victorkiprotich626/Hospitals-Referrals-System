package com.example.referrals.superadmin.service;

import com.example.referrals.hospital.Hospital;
import com.example.referrals.hospital.HospitalRepository;
import com.example.referrals.superadmin.form.HospitalForm;
import com.example.referrals.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class SuperAdminHospitalService {

    private final HospitalRepository hospitalRepository;
    private final UserRepository userRepository;

    public SuperAdminHospitalService(HospitalRepository hospitalRepository, UserRepository userRepository) {
        this.hospitalRepository = hospitalRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<Hospital> findAll() {
        return hospitalRepository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public List<Hospital> findAll(String query) {
        String normalizedQuery = normalizeQuery(query);
        if (normalizedQuery == null) {
            return findAll();
        }
        return hospitalRepository.findAllByOrderByNameAsc().stream()
            .filter(hospital -> matchesHospital(hospital, normalizedQuery))
            .toList();
    }

    @Transactional(readOnly = true)
    public HospitalForm buildForm(Long hospitalId) {
        Hospital hospital = hospitalRepository.findById(hospitalId)
            .orElseThrow(() -> new EntityNotFoundException("Hospital not found"));
        HospitalForm form = new HospitalForm();
        form.setName(hospital.getName());
        form.setCode(hospital.getCode());
        form.setContactEmail(hospital.getContactEmail());
        form.setContactPhone(hospital.getContactPhone());
        form.setAddress(hospital.getAddress());
        form.setEnabled(hospital.isEnabled());
        return form;
    }

    public void create(HospitalForm form) {
        validateHospital(form, null);
        Hospital hospital = new Hospital();
        apply(form, hospital);
        hospitalRepository.save(hospital);
    }

    public void update(Long hospitalId, HospitalForm form) {
        validateHospital(form, hospitalId);
        Hospital hospital = hospitalRepository.findById(hospitalId)
            .orElseThrow(() -> new EntityNotFoundException("Hospital not found"));
        apply(form, hospital);
    }

    public void delete(Long hospitalId) {
        Hospital hospital = hospitalRepository.findById(hospitalId)
            .orElseThrow(() -> new EntityNotFoundException("Hospital not found"));
        if (userRepository.countByHospitalId(hospitalId) > 0) {
            throw new IllegalArgumentException("Delete or reassign the hospital's users before deleting the hospital.");
        }
        hospitalRepository.delete(hospital);
    }

    private void validateHospital(HospitalForm form, Long existingId) {
        String normalizedCode = normalize(form.getCode());
        String normalizedName = normalize(form.getName());

        boolean duplicateCode = existingId == null
            ? hospitalRepository.existsByCodeIgnoreCase(normalizedCode)
            : hospitalRepository.existsByCodeIgnoreCaseAndIdNot(normalizedCode, existingId);
        if (duplicateCode) {
            throw new IllegalArgumentException("Hospital code already exists.");
        }

        boolean duplicateName = existingId == null
            ? hospitalRepository.existsByNameIgnoreCase(normalizedName)
            : hospitalRepository.existsByNameIgnoreCaseAndIdNot(normalizedName, existingId);
        if (duplicateName) {
            throw new IllegalArgumentException("Hospital name already exists.");
        }
    }

    private void apply(HospitalForm form, Hospital hospital) {
        hospital.setName(normalize(form.getName()));
        hospital.setCode(normalize(form.getCode()).toUpperCase());
        hospital.setContactEmail(normalizeNullable(form.getContactEmail()));
        hospital.setContactPhone(normalizeNullable(form.getContactPhone()));
        hospital.setAddress(normalizeNullable(form.getAddress()));
        hospital.setEnabled(form.isEnabled());
    }

    private String normalize(String value) {
        return value.trim();
    }

    private String normalizeNullable(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private boolean matchesHospital(Hospital hospital, String query) {
        return contains(hospital.getName(), query)
            || contains(hospital.getCode(), query)
            || contains(hospital.getContactEmail(), query)
            || contains(hospital.getContactPhone(), query)
            || contains(hospital.getAddress(), query);
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
}
