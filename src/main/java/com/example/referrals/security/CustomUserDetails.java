package com.example.referrals.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

public class CustomUserDetails implements UserDetails {

    private final Long userId;
    private final Long hospitalId;
    private final Long departmentId;
    private final Long doctorProfileId;
    private final String fullName;
    private final String username;
    private final String password;
    private final boolean enabled;
    private final boolean accountNonLocked;
    private final Collection<? extends GrantedAuthority> authorities;

    public CustomUserDetails(Long userId,
                             Long hospitalId,
                             Long departmentId,
                             Long doctorProfileId,
                             String fullName,
                             String username,
                             String password,
                             boolean enabled,
                             boolean accountNonLocked,
                             Collection<? extends GrantedAuthority> authorities) {
        this.userId = userId;
        this.hospitalId = hospitalId;
        this.departmentId = departmentId;
        this.doctorProfileId = doctorProfileId;
        this.fullName = fullName;
        this.username = username;
        this.password = password;
        this.enabled = enabled;
        this.accountNonLocked = accountNonLocked;
        this.authorities = authorities;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getHospitalId() {
        return hospitalId;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public Long getDoctorProfileId() {
        return doctorProfileId;
    }

    public String getFullName() {
        return fullName;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
