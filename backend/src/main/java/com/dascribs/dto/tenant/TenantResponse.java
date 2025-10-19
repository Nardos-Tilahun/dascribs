package com.dascribs.dto.tenant;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TenantResponse {

    private Long id;
    private String tenantId;
    private String name;
    private String domain;
    private Tenant.Plan plan;
    private Integer maxUsers;
    private Integer maxProperties;
    private String contactEmail;
    private String contactPhone;
    private String address;
    private String logoUrl;
    private boolean active;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime subscriptionEndsAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    private Long userCount;
    private Long propertyCount;

    // Constructors
    public TenantResponse() {}

    public TenantResponse(Tenant.Tenant tenant) {
        this.id = tenant.getId();
        this.tenantId = tenant.getTenantId();
        this.name = tenant.getName();
        this.domain = tenant.getDomain();
        this.plan = tenant.getPlan();
        this.maxUsers = tenant.getMaxUsers();
        this.maxProperties = tenant.getMaxProperties();
        this.contactEmail = tenant.getContactEmail();
        this.contactPhone = tenant.getContactPhone();
        this.address = tenant.getAddress();
        this.logoUrl = tenant.getLogoUrl();
        this.active = tenant.isActive();
        this.subscriptionEndsAt = tenant.getSubscriptionEndsAt();
        this.createdAt = tenant.getCreatedAt();
        this.updatedAt = tenant.getUpdatedAt();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public Tenant.Tenant.Plan getPlan() {
        return plan;
    }

    public void setPlan(Tenant.Tenant.Plan plan) {
        this.plan = plan;
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

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getSubscriptionEndsAt() {
        return subscriptionEndsAt;
    }

    public void setSubscriptionEndsAt(LocalDateTime subscriptionEndsAt) {
        this.subscriptionEndsAt = subscriptionEndsAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getUserCount() {
        return userCount;
    }

    public void setUserCount(Long userCount) {
        this.userCount = userCount;
    }

    public Long getPropertyCount() {
        return propertyCount;
    }

    public void setPropertyCount(Long propertyCount) {
        this.propertyCount = propertyCount;
    }

    // Helper methods
    public boolean isSubscriptionActive() {
        return subscriptionEndsAt == null || subscriptionEndsAt.isAfter(LocalDateTime.now());
    }

    public boolean hasReachedUserLimit() {
        return userCount != null && maxUsers != null && userCount >= maxUsers;
    }

    public boolean hasReachedPropertyLimit() {
        return propertyCount != null && maxProperties != null && propertyCount >= maxProperties;
    }

    public String getPlanDescription() {
        return plan != null ? plan.name() : "UNKNOWN";
    }

    @Override
    public String toString() {
        return "TenantResponse{" +
                "id=" + id +
                ", tenantId='" + tenantId + '\'' +
                ", name='" + name + '\'' +
                ", domain='" + domain + '\'' +
                ", plan=" + plan +
                ", maxUsers=" + maxUsers +
                ", maxProperties=" + maxProperties +
                ", contactEmail='" + contactEmail + '\'' +
                ", contactPhone='" + contactPhone + '\'' +
                ", address='" + address + '\'' +
                ", logoUrl='" + logoUrl + '\'' +
                ", active=" + active +
                ", subscriptionEndsAt=" + subscriptionEndsAt +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", userCount=" + userCount +
                ", propertyCount=" + propertyCount +
                '}';
    }
}