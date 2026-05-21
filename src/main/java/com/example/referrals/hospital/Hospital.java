package com.example.referrals.hospital;

import com.example.referrals.common.model.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "hospitals")
public class Hospital extends AuditableEntity {

    @Column(nullable = false, unique = true, length = 120)
    private String name;

    // we mzee
    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @Column(length = 120)
    private String contactEmail;

    @Column(length = 40)
    private String contactPhone;

    @Column(length = 255)
    private String address;

    @Column(nullable = false)
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

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
