package com.example.referrals.user;

import com.example.referrals.common.model.AuditableEntity;
import com.example.referrals.common.model.RoleName;
import com.example.referrals.directory.Department;
import com.example.referrals.directory.Doctor;
import com.example.referrals.hospital.Hospital;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "app_users")
public class AppUser extends AuditableEntity {

    @Column(nullable = false, length = 80)
    private String firstName;

    @Column(nullable = false, length = 80)
    private String lastName;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false, length = 255)
    private String passwordHash;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false)
    private boolean accountLocked = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hospital_id")
    private Hospital hospital;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_profile_id")
    private Doctor doctorProfile;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role_name", nullable = false, length = 50)
    private Set<RoleName> roles = new HashSet<>();

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isAccountLocked() {
        return accountLocked;
    }

    public void setAccountLocked(boolean accountLocked) {
        this.accountLocked = accountLocked;
    }

    public Hospital getHospital() {
        return hospital;
    }

    public void setHospital(Hospital hospital) {
        this.hospital = hospital;
    }

    public Set<RoleName> getRoles() {
        return roles;
    }

    public void setRoles(Set<RoleName> roles) {
        this.roles = roles;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Doctor getDoctorProfile() {
        return doctorProfile;
    }

    public void setDoctorProfile(Doctor doctorProfile) {
        this.doctorProfile = doctorProfile;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public RoleName getPrimaryRole() {
        if (roles.contains(RoleName.SUPER_ADMIN)) {
            return RoleName.SUPER_ADMIN;
        }
        if (roles.contains(RoleName.HOSPITAL_ADMIN)) {
            return RoleName.HOSPITAL_ADMIN;
        }
        if (roles.contains(RoleName.REFERRAL_OFFICER)) {
            return RoleName.REFERRAL_OFFICER;
        }
        if (roles.contains(RoleName.DOCTOR)) {
            return RoleName.DOCTOR;
        }
        if (roles.contains(RoleName.VIEWER)) {
            return RoleName.VIEWER;
        }
        return null;
    }
}
