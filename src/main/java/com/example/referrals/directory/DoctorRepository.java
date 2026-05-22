package com.example.referrals.directory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    @Query("""
        select d
        from Doctor d
        left join fetch d.department dept
        where d.hospital.id = :hospitalId
        order by d.lastName asc, d.firstName asc
        """)
    List<Doctor> findAllByHospitalIdOrderByLastNameAscFirstNameAsc(Long hospitalId);

    @Query("""
        select d
        from Doctor d
        left join fetch d.department dept
        where d.hospital.id = :hospitalId and d.enabled = true
        order by d.lastName asc, d.firstName asc
        """)
    List<Doctor> findAllEnabledByHospitalId(Long hospitalId);
