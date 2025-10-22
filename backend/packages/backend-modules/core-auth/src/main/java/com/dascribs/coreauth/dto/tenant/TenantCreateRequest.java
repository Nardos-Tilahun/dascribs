package com.dascribs.coreauth.dto.tenant;

import com.dascribs.coreauth.entity.tenant.Tenant;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class TenantCreateRequest {

    @NotBlank(message = "Tenant ID is required")
    @Size(min = 3, max = 100, message = "Tenant ID must be between 3 and 100 characters")
    private String tenantId;

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 255, message = "Name must be between 2 and 255 characters")
    private String name;

    private String domain;

    private Tenant.Plan plan = Tenant.Plan.FREE;

    @Email(message = "Contact email should be valid")
    private String contactEmail;

    @Size(max = 50, message = "Contact phone must not exceed 50 characters")
    private String contactPhone;

    private String address;

    private Integer maxUsers = 10;

    private Integer maxProperties = 100;

    // Constructors
    public TenantCreateRequest() {}

    public TenantCreateRequest(String tenantId, String name, String domain, Tenant.Plan plan) {
        this.tenantId = tenantId;
        this.name = name;
        this.domain = domain;
        this.plan = plan;
    }

    // Getters and Setters
    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

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

    // Business validation
    public boolean isValidPlan() {
        return plan != null;
    }

    public boolean hasValidLimits() {
        return maxUsers != null && maxUsers > 0 &&
                maxProperties != null && maxProperties > 0;
    }

    @Override
    public String toString() {
        return "TenantCreateRequest{" +
                "tenantId='" + tenantId + '\'' +
                ", name='" + name + '\'' +
                ", domain='" + domain + '\'' +
                ", plan=" + plan +
                ", contactEmail='" + contactEmail + '\'' +
                ", contactPhone='" + contactPhone + '\'' +
                ", address='" + address + '\'' +
                ", maxUsers=" + maxUsers +
                ", maxProperties=" + maxProperties +
                '}';
    }
}