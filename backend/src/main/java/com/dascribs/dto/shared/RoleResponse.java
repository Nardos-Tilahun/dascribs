package com.dascribs.dto.shared;

import com.dascribs.model.user.Permission;
import com.dascribs.model.user.Role;

import java.util.List;
import java.util.stream.Collectors;

public class RoleResponse {

    private String name;
    private String description;
    private List<String> permissions;

    // Constructors
    public RoleResponse() {}

    public RoleResponse(Role role) {
        this.name = role.name();
        this.description = role.getDescription();
        this.permissions = role.getPermissions().stream()
                .map(Enum::name)
                .collect(Collectors.toList());
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }

    // Helper methods
    public boolean hasPermission(String permission) {
        return permissions != null && permissions.contains(permission);
    }

    public boolean hasAllPermissions(List<String> requiredPermissions) {
        return permissions != null && permissions.containsAll(requiredPermissions);
    }

    public int getPermissionCount() {
        return permissions != null ? permissions.size() : 0;
    }

    @Override
    public String toString() {
        return "RoleResponse{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", permissions=" + permissions +
                '}';
    }
}