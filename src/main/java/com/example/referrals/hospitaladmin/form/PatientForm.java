package com.example.referrals.hospitaladmin.form;

import com.example.referrals.patient.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class PatientForm {

    @Size(max = 40)
    private String patientNumber;

    @NotBlank(message = "First name is required")
    @Size(max = 80)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 80)
    private String lastName;

    @NotNull(message = "Select a gender")
    private Gender gender;

    @NotNull(message = "Date of birth is required")
    @PastOrPresent(message = "Date of birth cannot be in the future")
    private LocalDate dateOfBirth;

    @Size(max = 40)
    private String phoneNumber;

    @Size(max = 40)
    private String nationalId;

    public String getPatientNumber() {
        return patientNumber;
    }

    public void setPatientNumber(String patientNumber) {
        this.patientNumber = patientNumber;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }
