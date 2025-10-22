package com.dascribs.coreauth.dto.user;

import com.dascribs.coreauth.entity.user.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class UserUpdateRequest {

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 255, message = "Full name must be between 2 and 255 characters")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    @Size(max = 50, message = "Phone number must not exceed 50 characters")
    private String phone;

    @NotNull(message = "Role is required")
    private Role role;

    private BigDecimal salary;

    private BigDecimal commissionRate;

    private Boolean active;

    // Constructors
    public UserUpdateRequest() {}

    // Getters and Setters
    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public BigDecimal getSalary() {
        return salary;
    }

    public void setSalary(BigDecimal salary) {
        this.salary = salary;
    }

    public BigDecimal getCommissionRate() {
        return commissionRate;
    }

    public void setCommissionRate(BigDecimal commissionRate) {
        this.commissionRate = commissionRate;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    // Helper methods for partial updates
    public boolean hasFullName() {
        return fullName != null && !fullName.trim().isEmpty();
    }

    public boolean hasEmail() {
        return email != null && !email.trim().isEmpty();
    }

    public boolean hasPhone() {
        return phone != null;
    }

    public boolean hasRole() {
        return role != null;
    }

    public boolean hasSalary() {
        return salary != null;
    }

    public boolean hasCommissionRate() {
        return commissionRate != null;
    }

    public boolean hasActive() {
        return active != null;
    }

    @Override
    public String toString() {
        return "UserUpdateRequest{" +
                "fullName='" + fullName + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", role=" + role +
                ", salary=" + salary +
                ", commissionRate=" + commissionRate +
                ", active=" + active +
                '}';
    }
}