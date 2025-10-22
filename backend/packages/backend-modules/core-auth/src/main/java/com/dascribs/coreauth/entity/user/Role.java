package com.dascribs.coreauth.entity.user;

import java.util.Set;

public enum Role {
    SUPER_ADMIN(
            "System Administrator",
            Set.of(
                    Permission.USER_READ, Permission.USER_WRITE, Permission.USER_DELETE,
                    Permission.ROLE_READ, Permission.ROLE_WRITE, Permission.ROLE_DELETE,
                    Permission.TENANT_READ, Permission.TENANT_WRITE, Permission.TENANT_DELETE,
                    Permission.PROPERTY_READ, Permission.PROPERTY_WRITE, Permission.PROPERTY_DELETE,
                    Permission.LEAD_READ, Permission.LEAD_WRITE, Permission.LEAD_DELETE,
                    Permission.APPOINTMENT_READ, Permission.APPOINTMENT_WRITE, Permission.APPOINTMENT_DELETE,
                    Permission.DEAL_READ, Permission.DEAL_WRITE, Permission.DEAL_DELETE,
                    Permission.ANALYTICS_READ, Permission.ANALYTICS_WRITE,
                    Permission.SYSTEM_SETTINGS_READ, Permission.SYSTEM_SETTINGS_WRITE
            )
    ),

    ADMIN(
            "Organization Administrator",
            Set.of(
                    Permission.USER_READ, Permission.USER_WRITE,
                    Permission.PROPERTY_READ, Permission.PROPERTY_WRITE, Permission.PROPERTY_DELETE,
                    Permission.LEAD_READ, Permission.LEAD_WRITE, Permission.LEAD_DELETE,
                    Permission.APPOINTMENT_READ, Permission.APPOINTMENT_WRITE, Permission.APPOINTMENT_DELETE,
                    Permission.DEAL_READ, Permission.DEAL_WRITE, Permission.DEAL_DELETE,
                    Permission.ANALYTICS_READ
            )
    ),

    AGENT(
            "Sales Agent",
            Set.of(
                    Permission.LEAD_READ, Permission.LEAD_WRITE,
                    Permission.APPOINTMENT_READ, Permission.APPOINTMENT_WRITE,
                    Permission.PROPERTY_READ,
                    Permission.DEAL_READ, Permission.DEAL_WRITE
            )
    ),

    CLIENT(
            "Property Client",
            Set.of(
                    Permission.PROPERTY_READ,
                    Permission.APPOINTMENT_READ
            )
    );

    private final String description;
    private final Set<Permission> permissions;

    Role(String description, Set<Permission> permissions) {
        this.description = description;
        this.permissions = permissions;
    }

    public String getDescription() {
        return description;
    }

    public Set<Permission> getPermissions() {
        return permissions;
    }
}