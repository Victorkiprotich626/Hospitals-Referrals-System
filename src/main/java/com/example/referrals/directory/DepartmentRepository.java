package com.example.referrals.directory;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

    List<Department> findAllByHospitalIdOrderByNameAsc(Long hospitalId);

    List<Department> findAllByHospitalIdAndEnabledTrueOrderByNameAsc(Long hospitalId);

    Optional<Department> findByIdAndHospitalId(Long departmentId, Long hospitalId);

    boolean existsByHospitalIdAndCodeIgnoreCase(Long hospitalId, String code);

    boolean existsByHospitalIdAndCodeIgnoreCaseAndIdNot(Long hospitalId, String code, Long departmentId);

    boolean existsByHospitalIdAndNameIgnoreCase(Long hospitalId, String name);

    boolean existsByHospitalIdAndNameIgnoreCaseAndIdNot(Long hospitalId, String name, Long departmentId);

    long countByHospitalIdAndEnabledTrue(Long hospitalId);
}
