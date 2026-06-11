package com.example.referrals.superadmin.form;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class HospitalForm {

    @NotBlank(message = "Hospital name is required")
    @Size(max = 120)
    private String name;

    @NotBlank(message = "Hospital code is required")
    @Size(max = 40)
    private String code;

    @Email(message = "Enter a valid email address")
    @Size(max = 120)
    private String contactEmail;

    @Size(max = 40)
    private String contactPhone;

    @Size(max = 255)
    private String address;

    private boolean enabled = true;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
