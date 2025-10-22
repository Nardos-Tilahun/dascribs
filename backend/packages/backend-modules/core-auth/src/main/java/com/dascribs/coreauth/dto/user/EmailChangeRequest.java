package com.dascribs.coreauth.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class EmailChangeRequest {
    @NotBlank(message = "New email is required")
    @Email(message = "New email should be valid")
    private String newEmail;

    @NotBlank(message = "Password confirmation is required")
    private String password; // Require password for security

    // Getters and setters
    public String getNewEmail() { return newEmail; }
    public void setNewEmail(String newEmail) { this.newEmail = newEmail; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
