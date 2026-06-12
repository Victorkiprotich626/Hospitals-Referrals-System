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

    public void create(HospitalUserForm form) {
        Long tenantId = requireTenantId();
        validateForm(form, tenantId, null, true);

        Hospital hospital = hospitalRepository.findById(tenantId)
            .orElseThrow(() -> new EntityNotFoundException("Hospital not found"));

        AppUser user = new AppUser();
        user.setHospital(hospital);
        apply(form, user, tenantId, true);
        userRepository.save(user);
    }

    public void update(Long userId, HospitalUserForm form) {
        Long tenantId = requireTenantId();
        validateForm(form, tenantId, userId, false);

        AppUser user = userRepository.findById(userId)
            .filter(existing -> existing.getHospital() != null && existing.getHospital().getId().equals(tenantId))
            .orElseThrow(() -> new EntityNotFoundException("User not found"));
        apply(form, user, tenantId, false);
    }

    public void delete(Long userId) {
        AppUser user = userRepository.findById(userId)
            .filter(existing -> existing.getHospital() != null && existing.getHospital().getId().equals(requireTenantId()))
            .orElseThrow(() -> new EntityNotFoundException("User not found"));
        userRepository.delete(user);
    }

    private void validateForm(HospitalUserForm form, Long tenantId, Long userId, boolean creating) {
        if (!MANAGED_ROLES.contains(form.getRole())) {
            throw new IllegalArgumentException("This role cannot be managed from the hospital workspace.");
        }

        boolean duplicateEmail = userId == null
            ? userRepository.existsByEmailIgnoreCase(form.getEmail().trim())
            : userRepository.existsByEmailIgnoreCaseAndIdNot(form.getEmail().trim(), userId);
        if (duplicateEmail) {
            throw new IllegalArgumentException("Email address already exists.");
        }

        if (creating && !StringUtils.hasText(form.getPassword())) {
            throw new IllegalArgumentException("A password is required when creating a user.");
        }

        Department department = resolveDepartment(form.getDepartmentId(), tenantId);
        Doctor doctor = resolveDoctor(form.getDoctorProfileId(), tenantId);

        if (form.getRole() == RoleName.DOCTOR && doctor == null) {
            throw new IllegalArgumentException("Doctor users must be linked to a doctor profile.");
        }

        if (doctor != null && doctor.getDepartment() != null && department != null
            && !doctor.getDepartment().getId().equals(department.getId())) {
            throw new IllegalArgumentException("Selected doctor does not belong to the chosen department.");
        }
    }

    private void apply(HospitalUserForm form, AppUser user, Long tenantId, boolean creating) {
        Department department = resolveDepartment(form.getDepartmentId(), tenantId);
        Doctor doctor = resolveDoctor(form.getDoctorProfileId(), tenantId);

        user.setFirstName(form.getFirstName().trim());
        user.setLastName(form.getLastName().trim());
        user.setEmail(form.getEmail().trim().toLowerCase());
        user.setEnabled(form.isEnabled());
        user.setRoles(Set.of(form.getRole()));

        if (doctor != null && doctor.getDepartment() != null && department == null) {
            department = doctor.getDepartment();
        }
        user.setDepartment(department);
        user.setDoctorProfile(form.getRole() == RoleName.DOCTOR ? doctor : null);

        if (creating || StringUtils.hasText(form.getPassword())) {
            user.setPasswordHash(passwordEncoder.encode(form.getPassword().trim()));
        }
    }

    private Department resolveDepartment(Long departmentId, Long tenantId) {
        if (departmentId == null) {
            return null;
        }
        return departmentRepository.findByIdAndHospitalId(departmentId, tenantId)
            .filter(Department::isEnabled)
            .orElseThrow(() -> new IllegalArgumentException("Selected department is invalid or disabled."));
    }

    private Doctor resolveDoctor(Long doctorId, Long tenantId) {
        if (doctorId == null) {
            return null;
        }
        return doctorRepository.findByIdAndHospitalId(doctorId, tenantId)
            .filter(Doctor::isEnabled)
            .orElseThrow(() -> new IllegalArgumentException("Selected doctor is invalid or disabled."));
    }

    private Long requireTenantId() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant context not available");
        }
        return tenantId;
    }

    private boolean matchesUser(AppUser user, String query) {
        return contains(user.getFirstName(), query)
            || contains(user.getLastName(), query)
            || contains(user.getFullName(), query)
            || contains(user.getEmail(), query)
            || (user.getPrimaryRole() != null && contains(user.getPrimaryRole().name(), query))
            || (user.getDepartment() != null && contains(user.getDepartment().getName(), query))
            || (user.getDoctorProfile() != null && contains(user.getDoctorProfile().getFullName(), query));
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
