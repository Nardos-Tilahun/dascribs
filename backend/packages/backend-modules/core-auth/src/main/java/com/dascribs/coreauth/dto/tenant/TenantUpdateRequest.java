package com.dascribs.coreauth.dto.tenant;

import com.dascribs.coreauth.entity.tenant.Tenant;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class TenantUpdateRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 255, message = "Name must be between 2 and 255 characters")
    private String name;

    private String domain;

    private Tenant.Plan plan;

    @Email(message = "Contact email should be valid")
    private String contactEmail;

    @Size(max = 50, message = "Contact phone must not exceed 50 characters")
    private String contactPhone;

    private String address;

    private Integer maxUsers;

    private Integer maxProperties;

    private Boolean active;

    // Constructors
    public TenantUpdateRequest() {}

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public Tenant.Plan getPlan() {
        return plan;
    }

    public void setPlan(Tenant.Plan plan) {
        this.plan = plan;
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

    public Integer getMaxUsers() {
        return maxUsers;
    }

    public void setMaxUsers(Integer maxUsers) {
        this.maxUsers = maxUsers;
    }

    public Integer getMaxProperties() {
        return maxProperties;
    }

    public void setMaxProperties(Integer maxProperties) {
        this.maxProperties = maxProperties;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    // Helper methods for partial updates
    public boolean hasName() {
        return name != null && !name.trim().isEmpty();
    }

    public boolean hasDomain() {
        return domain != null;
    }

    public boolean hasPlan() {
        return plan != null;
    }

    public boolean hasContactEmail() {
        return contactEmail != null;
    }

    public boolean hasContactPhone() {
        return contactPhone != null;
    }

    public boolean hasAddress() {
        return address != null;
    }

    public boolean hasMaxUsers() {
        return maxUsers != null;
    }

    public boolean hasMaxProperties() {
        return maxProperties != null;
    }

    public boolean hasActive() {
        return active != null;
    }

    @Override
    public String toString() {
        return "TenantUpdateRequest{" +
                "name='" + name + '\'' +
                ", domain='" + domain + '\'' +
                ", plan=" + plan +
                ", contactEmail='" + contactEmail + '\'' +
                ", contactPhone='" + contactPhone + '\'' +
                ", address='" + address + '\'' +
                ", maxUsers=" + maxUsers +
                ", maxProperties=" + maxProperties +
                ", active=" + active +
                '}';
    }
}