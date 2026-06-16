package com.example.referrals.superadmin.service;

import com.example.referrals.common.model.RoleName;
import com.example.referrals.hospital.Hospital;
import com.example.referrals.hospital.HospitalRepository;
import com.example.referrals.superadmin.form.HospitalAdminForm;
import com.example.referrals.user.AppUser;
import com.example.referrals.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@Transactional
public class SuperAdminUserService {

    private final UserRepository userRepository;
    private final HospitalRepository hospitalRepository;
    private final PasswordEncoder passwordEncoder;

    public SuperAdminUserService(UserRepository userRepository,
                                 HospitalRepository hospitalRepository,
                                 PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.hospitalRepository = hospitalRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<AppUser> findHospitalAdmins() {
        return userRepository.findAllByRole(RoleName.HOSPITAL_ADMIN);
    }

    @Transactional(readOnly = true)
    public List<AppUser> findHospitalAdmins(String query) {
        String normalizedQuery = normalizeQuery(query);
        if (normalizedQuery == null) {
            return findHospitalAdmins();
        }
        return userRepository.findAllByRole(RoleName.HOSPITAL_ADMIN).stream()
            .filter(admin -> matchesAdmin(admin, normalizedQuery))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<Hospital> findHospitalsForSelection() {
        return hospitalRepository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public HospitalAdminForm buildForm(Long userId) {
        AppUser user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("Hospital admin not found"));

        HospitalAdminForm form = new HospitalAdminForm();
        form.setFirstName(user.getFirstName());
        form.setLastName(user.getLastName());
        form.setEmail(user.getEmail());
        form.setEnabled(user.isEnabled());
        if (user.getHospital() != null) {
            form.setHospitalId(user.getHospital().getId());
        }
        return form;
    }

    public void createHospitalAdmin(HospitalAdminForm form) {
        validateUser(form, null, true);
        AppUser user = new AppUser();
        apply(form, user, true);
        user.setRoles(Set.of(RoleName.HOSPITAL_ADMIN));
        userRepository.save(user);
    }

    public void updateHospitalAdmin(Long userId, HospitalAdminForm form) {
        validateUser(form, userId, false);
        AppUser user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("Hospital admin not found"));
        apply(form, user, false);
    }

    public void deleteHospitalAdmin(Long userId) {
        AppUser user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("Hospital admin not found"));
        userRepository.delete(user);
    }

    public long countEnabledAdminsForHospital(Long hospitalId) {
        return userRepository.countByHospitalIdAndEnabledTrue(hospitalId);
    }

    private void validateUser(HospitalAdminForm form, Long existingId, boolean creating) {
        boolean duplicateEmail = existingId == null
            ? userRepository.existsByEmailIgnoreCase(form.getEmail().trim())
            : userRepository.existsByEmailIgnoreCaseAndIdNot(form.getEmail().trim(), existingId);
        if (duplicateEmail) {
            throw new IllegalArgumentException("Email address already exists.");
        }
        if (creating && !StringUtils.hasText(form.getPassword())) {
            throw new IllegalArgumentException("A password is required when creating a hospital admin.");
        }
        hospitalRepository.findById(form.getHospitalId())
            .orElseThrow(() -> new IllegalArgumentException("Selected hospital does not exist."));
    }

    private void apply(HospitalAdminForm form, AppUser user, boolean creating) {
        Hospital hospital = hospitalRepository.findById(form.getHospitalId())
            .orElseThrow(() -> new EntityNotFoundException("Hospital not found"));
        user.setFirstName(form.getFirstName().trim());
        user.setLastName(form.getLastName().trim());
        user.setEmail(form.getEmail().trim().toLowerCase());
        user.setHospital(hospital);
        user.setEnabled(form.isEnabled());
        if (creating || StringUtils.hasText(form.getPassword())) {
            user.setPasswordHash(passwordEncoder.encode(form.getPassword().trim()));
        }
    }

    private boolean matchesAdmin(AppUser admin, String query) {
        return contains(admin.getFirstName(), query)
            || contains(admin.getLastName(), query)
            || contains(admin.getFullName(), query)
            || contains(admin.getEmail(), query)
            || (admin.getHospital() != null
                && (contains(admin.getHospital().getName(), query) || contains(admin.getHospital().getCode(), query)));
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
