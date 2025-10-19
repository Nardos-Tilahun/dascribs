package com.dascribs.model.user;

public enum Permission {
    // User Management
    USER_READ,
    USER_WRITE,
    USER_DELETE,

    // Role Management
    ROLE_READ,
    ROLE_WRITE,
    ROLE_DELETE,

    // Tenant Management
    TENANT_READ,
    TENANT_WRITE,
    TENANT_DELETE,

    // Property Management
    PROPERTY_READ,
    PROPERTY_WRITE,
    PROPERTY_DELETE,

    // Lead Management
    LEAD_READ,
    LEAD_WRITE,
    LEAD_DELETE,

    // Appointment Management
    APPOINTMENT_READ,
    APPOINTMENT_WRITE,
    APPOINTMENT_DELETE,

    // Deal Management
    DEAL_READ,
    DEAL_WRITE,
    DEAL_DELETE,

    // Analytics
    ANALYTICS_READ,
    ANALYTICS_WRITE,

    // System Settings
    SYSTEM_SETTINGS_READ,
    SYSTEM_SETTINGS_WRITE
}