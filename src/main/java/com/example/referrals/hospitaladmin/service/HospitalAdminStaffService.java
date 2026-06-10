package com.example.referrals.hospitaladmin.service;

import com.example.referrals.common.model.RoleName;
import com.example.referrals.directory.Department;
import com.example.referrals.directory.DepartmentRepository;
import com.example.referrals.directory.Doctor;
import com.example.referrals.directory.DoctorRepository;
import com.example.referrals.hospital.Hospital;
import com.example.referrals.hospital.HospitalRepository;
import com.example.referrals.hospitaladmin.form.HospitalUserForm;
import com.example.referrals.tenant.TenantContext;
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
public class HospitalAdminStaffService {

    private static final Set<RoleName> MANAGED_ROLES = Set.of(
        RoleName.REFERRAL_OFFICER,
        RoleName.DOCTOR,
        RoleName.VIEWER
    );

    private final UserRepository userRepository;
    private final HospitalRepository hospitalRepository;
    private final DepartmentRepository departmentRepository;
    private final DoctorRepository doctorRepository;
    private final PasswordEncoder passwordEncoder;

    public HospitalAdminStaffService(UserRepository userRepository,
                                     HospitalRepository hospitalRepository,
                                     DepartmentRepository departmentRepository,
                                     DoctorRepository doctorRepository,
                                     PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.hospitalRepository = hospitalRepository;
        this.departmentRepository = departmentRepository;
        this.doctorRepository = doctorRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<AppUser> findManagedUsersForCurrentTenant() {
        return userRepository.findAllByHospitalIdAndRoles(requireTenantId(), MANAGED_ROLES);
    }

    @Transactional(readOnly = true)
    public List<AppUser> findManagedUsersForCurrentTenant(String query, RoleName role) {
        String normalizedQuery = normalizeQuery(query);
        return userRepository.findAllByHospitalIdAndRoles(requireTenantId(), MANAGED_ROLES).stream()
            .filter(user -> role == null || role.equals(user.getPrimaryRole()))
            .filter(user -> normalizedQuery == null || matchesUser(user, normalizedQuery))
            .toList();
    }

    @Transactional(readOnly = true)
    public HospitalUserForm buildForm(Long userId) {
        AppUser user = userRepository.findById(userId)
            .filter(existing -> existing.getHospital() != null && existing.getHospital().getId().equals(requireTenantId()))
            .orElseThrow(() -> new EntityNotFoundException("User not found"));

        HospitalUserForm form = new HospitalUserForm();
        form.setFirstName(user.getFirstName());
        form.setLastName(user.getLastName());
        form.setEmail(user.getEmail());
        form.setRole(user.getPrimaryRole());
        form.setEnabled(user.isEnabled());
        if (user.getDepartment() != null) {
            form.setDepartmentId(user.getDepartment().getId());
        }
        if (user.getDoctorProfile() != null) {
            form.setDoctorProfileId(user.getDoctorProfile().getId());
        }
        return form;
    }
