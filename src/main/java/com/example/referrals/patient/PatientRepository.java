package com.example.referrals.patient;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {

    List<Patient> findAllByHospitalIdOrderByLastNameAscFirstNameAsc(Long hospitalId);

    Optional<Patient> findByIdAndHospitalId(Long patientId, Long hospitalId);

    boolean existsByPatientNumberIgnoreCaseAndHospitalId(String patientNumber, Long hospitalId);

    boolean existsByPatientNumberIgnoreCaseAndHospitalIdAndIdNot(String patientNumber, Long hospitalId, Long patientId);

    Optional<Patient> findByHospitalIdAndNationalIdIgnoreCase(Long hospitalId, String nationalId);

    Optional<Patient> findByHospitalIdAndFirstNameIgnoreCaseAndLastNameIgnoreCaseAndDateOfBirth(Long hospitalId,
                                                                                                String firstName,
                                                                                                String lastName,
                                                                                                LocalDate dateOfBirth);

    long countByHospitalId(Long hospitalId);
}
