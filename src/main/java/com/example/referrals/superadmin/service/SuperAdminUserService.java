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
