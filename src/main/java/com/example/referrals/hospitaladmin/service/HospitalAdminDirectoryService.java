package com.example.referrals.hospitaladmin.service;

import com.example.referrals.directory.Department;
import com.example.referrals.directory.DepartmentRepository;
import com.example.referrals.directory.Doctor;
import com.example.referrals.directory.DoctorRepository;
import com.example.referrals.hospital.Hospital;
import com.example.referrals.hospital.HospitalRepository;
import com.example.referrals.hospitaladmin.form.DepartmentForm;
import com.example.referrals.hospitaladmin.form.DoctorForm;
import com.example.referrals.referral.ReferralRepository;
import com.example.referrals.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class HospitalAdminDirectoryService {

    private final DepartmentRepository departmentRepository;
    private final DoctorRepository doctorRepository;
    private final HospitalRepository hospitalRepository;
    private final ReferralRepository referralRepository;

    public HospitalAdminDirectoryService(DepartmentRepository departmentRepository,
                                         DoctorRepository doctorRepository,
                                         HospitalRepository hospitalRepository,
                                         ReferralRepository referralRepository) {
        this.departmentRepository = departmentRepository;
        this.doctorRepository = doctorRepository;
        this.hospitalRepository = hospitalRepository;
        this.referralRepository = referralRepository;
    }

    @Transactional(readOnly = true)
    public List<Department> findDepartmentsForCurrentTenant() {
        return departmentRepository.findAllByHospitalIdOrderByNameAsc(requireTenantId());
    }

    @Transactional(readOnly = true)
    public List<Department> findDepartmentsForCurrentTenant(String query) {
        String normalizedQuery = normalizeQuery(query);
        if (normalizedQuery == null) {
            return findDepartmentsForCurrentTenant();
        }
        return departmentRepository.findAllByHospitalIdOrderByNameAsc(requireTenantId()).stream()
            .filter(department -> contains(department.getName(), normalizedQuery)
                || contains(department.getCode(), normalizedQuery))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<Department> findEnabledDepartmentsForCurrentTenant() {
        return departmentRepository.findAllByHospitalIdAndEnabledTrueOrderByNameAsc(requireTenantId());
    }

    @Transactional(readOnly = true)
    public DepartmentForm buildDepartmentForm(Long departmentId) {
        Department department = departmentRepository.findByIdAndHospitalId(departmentId, requireTenantId())
            .orElseThrow(() -> new EntityNotFoundException("Department not found"));
        DepartmentForm form = new DepartmentForm();
        form.setName(department.getName());
        form.setCode(department.getCode());
        form.setEnabled(department.isEnabled());
        return form;
    }

    public void createDepartment(DepartmentForm form) {
        Long tenantId = requireTenantId();
        validateDepartment(form, tenantId, null);

        Hospital hospital = hospitalRepository.findById(tenantId)
            .orElseThrow(() -> new EntityNotFoundException("Hospital not found"));

        Department department = new Department();
        department.setHospital(hospital);
        apply(form, department);
        departmentRepository.save(department);
    }

    public void updateDepartment(Long departmentId, DepartmentForm form) {
        Long tenantId = requireTenantId();
        validateDepartment(form, tenantId, departmentId);
        Department department = departmentRepository.findByIdAndHospitalId(departmentId, tenantId)
            .orElseThrow(() -> new EntityNotFoundException("Department not found"));
        apply(form, department);
    }

    public void deleteDepartment(Long departmentId) {
        Department department = departmentRepository.findByIdAndHospitalId(departmentId, requireTenantId())
            .orElseThrow(() -> new EntityNotFoundException("Department not found"));
        doctorRepository.findAllByHospitalIdOrderByLastNameAscFirstNameAsc(requireTenantId()).stream()
            .filter(doctor -> doctor.getDepartment() != null && doctor.getDepartment().getId().equals(departmentId))
            .findAny()
            .ifPresent(doctor -> {
                throw new IllegalArgumentException("Reassign or delete doctors in this department before deleting it.");
            });
        if (referralRepository.existsByAssignedDepartmentId(departmentId)) {
            throw new IllegalArgumentException("This department is used by routed referrals and cannot be deleted yet.");
        }
        departmentRepository.delete(department);
    }

    @Transactional(readOnly = true)
    public List<Doctor> findDoctorsForCurrentTenant() {
        return doctorRepository.findAllByHospitalIdOrderByLastNameAscFirstNameAsc(requireTenantId());
    }

    @Transactional(readOnly = true)
    public List<Doctor> findDoctorsForCurrentTenant(String query, Long departmentId) {
        String normalizedQuery = normalizeQuery(query);
        return doctorRepository.findAllByHospitalIdOrderByLastNameAscFirstNameAsc(requireTenantId()).stream()
            .filter(doctor -> departmentId == null
                || (doctor.getDepartment() != null && departmentId.equals(doctor.getDepartment().getId())))
            .filter(doctor -> normalizedQuery == null || matchesDoctor(doctor, normalizedQuery))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<Doctor> findEnabledDoctorsForCurrentTenant() {
        return doctorRepository.findAllEnabledByHospitalId(requireTenantId());
    }

    @Transactional(readOnly = true)
    public DoctorForm buildDoctorForm(Long doctorId) {
        Doctor doctor = doctorRepository.findByIdAndHospitalId(doctorId, requireTenantId())
            .orElseThrow(() -> new EntityNotFoundException("Doctor not found"));
        DoctorForm form = new DoctorForm();
        form.setFirstName(doctor.getFirstName());
        form.setLastName(doctor.getLastName());
        form.setEmail(doctor.getEmail());
        form.setPhoneNumber(doctor.getPhoneNumber());
        form.setSpecialty(doctor.getSpecialty());
        form.setEnabled(doctor.isEnabled());
        if (doctor.getDepartment() != null) {
            form.setDepartmentId(doctor.getDepartment().getId());
        }
        return form;
    }

    public void createDoctor(DoctorForm form) {
        Long tenantId = requireTenantId();
        validateDoctor(form, tenantId, null);

        Hospital hospital = hospitalRepository.findById(tenantId)
            .orElseThrow(() -> new EntityNotFoundException("Hospital not found"));

        Doctor doctor = new Doctor();
        doctor.setHospital(hospital);
        apply(form, tenantId, doctor);
        doctorRepository.save(doctor);
    }

    public void updateDoctor(Long doctorId, DoctorForm form) {
        Long tenantId = requireTenantId();
        validateDoctor(form, tenantId, doctorId);
        Doctor doctor = doctorRepository.findByIdAndHospitalId(doctorId, tenantId)
            .orElseThrow(() -> new EntityNotFoundException("Doctor not found"));
        apply(form, tenantId, doctor);
    }

    public void deleteDoctor(Long doctorId) {
        Doctor doctor = doctorRepository.findByIdAndHospitalId(doctorId, requireTenantId())
            .orElseThrow(() -> new EntityNotFoundException("Doctor not found"));
        if (referralRepository.existsByAssignedDoctorId(doctorId)) {
            throw new IllegalArgumentException("This doctor is assigned to referrals and cannot be deleted yet.");
        }
        doctorRepository.delete(doctor);
    }

    @Transactional(readOnly = true)
    public long countEnabledDepartmentsForCurrentTenant() {
        return departmentRepository.countByHospitalIdAndEnabledTrue(requireTenantId());
    }

    @Transactional(readOnly = true)
    public long countEnabledDoctorsForCurrentTenant() {
        return doctorRepository.countByHospitalIdAndEnabledTrue(requireTenantId());
    }

    private void validateDepartment(DepartmentForm form, Long tenantId, Long departmentId) {
        String code = form.getCode().trim();
        String name = form.getName().trim();
        boolean duplicateCode = departmentId == null
            ? departmentRepository.existsByHospitalIdAndCodeIgnoreCase(tenantId, code)
            : departmentRepository.existsByHospitalIdAndCodeIgnoreCaseAndIdNot(tenantId, code, departmentId);
        if (duplicateCode) {
            throw new IllegalArgumentException("Department code already exists in this hospital.");
        }
        boolean duplicateName = departmentId == null
            ? departmentRepository.existsByHospitalIdAndNameIgnoreCase(tenantId, name)
            : departmentRepository.existsByHospitalIdAndNameIgnoreCaseAndIdNot(tenantId, name, departmentId);
        if (duplicateName) {
            throw new IllegalArgumentException("Department name already exists in this hospital.");
        }
    }

    private void validateDoctor(DoctorForm form, Long tenantId, Long doctorId) {
        String email = form.getEmail().trim().toLowerCase();
        boolean duplicateEmail = doctorId == null
            ? doctorRepository.existsByHospitalIdAndEmailIgnoreCase(tenantId, email)
            : doctorRepository.existsByHospitalIdAndEmailIgnoreCaseAndIdNot(tenantId, email, doctorId);
        if (duplicateEmail) {
            throw new IllegalArgumentException("Doctor email already exists in this hospital.");
        }
        if (form.getDepartmentId() != null) {
            Department department = departmentRepository.findByIdAndHospitalId(form.getDepartmentId(), tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Selected department does not belong to this hospital."));
            if (!department.isEnabled()) {
                throw new IllegalArgumentException("Selected department is disabled.");
            }
        }
    }

    private void apply(DepartmentForm form, Department department) {
        department.setName(form.getName().trim());
        department.setCode(form.getCode().trim().toUpperCase());
        department.setEnabled(form.isEnabled());
    }

    private void apply(DoctorForm form, Long tenantId, Doctor doctor) {
        doctor.setFirstName(form.getFirstName().trim());
        doctor.setLastName(form.getLastName().trim());
        doctor.setEmail(form.getEmail().trim().toLowerCase());
        doctor.setPhoneNumber(normalizeNullable(form.getPhoneNumber()));
        doctor.setSpecialty(normalizeNullable(form.getSpecialty()));
        doctor.setEnabled(form.isEnabled());
        if (form.getDepartmentId() == null) {
            doctor.setDepartment(null);
        } else {
            Department department = departmentRepository.findByIdAndHospitalId(form.getDepartmentId(), tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Selected department does not belong to this hospital."));
            doctor.setDepartment(department);
        }
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

    private boolean matchesDoctor(Doctor doctor, String query) {
        return contains(doctor.getFirstName(), query)
            || contains(doctor.getLastName(), query)
            || contains(doctor.getFullName(), query)
            || contains(doctor.getEmail(), query)
            || contains(doctor.getPhoneNumber(), query)
            || contains(doctor.getSpecialty(), query)
            || (doctor.getDepartment() != null && contains(doctor.getDepartment().getName(), query));
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
