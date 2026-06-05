package com.example.referrals.hospitaladmin.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ReferralNoteForm {

    @NotBlank(message = "Note is required")
    @Size(max = 500)
    private String note;

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
