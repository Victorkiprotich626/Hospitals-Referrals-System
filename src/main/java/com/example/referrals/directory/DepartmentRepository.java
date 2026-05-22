package com.example.referrals.directory;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

    List<Department> findAllByHospitalIdOrderByNameAsc(Long hospitalId);
