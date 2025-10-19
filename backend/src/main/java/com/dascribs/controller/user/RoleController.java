package com.dascribs.controller.user;

import com.dascribs.dto.response.ApiResponse;
import com.dascribs.dto.response.RoleResponse;
import com.dascribs.model.user.Permission;
import com.dascribs.model.user.Role;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/roles")
public class RoleController {

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<RoleResponse>>> getAllRoles() {
        try {
            List<RoleResponse> roles = Arrays.stream(Role.values())
                    .map(RoleResponse::new)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.success("Roles retrieved successfully", roles));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/permissions")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<String>>> getAllPermissions() {
        try {
            List<String> permissions = Arrays.stream(Permission.values())
                    .map(Enum::name)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(ApiResponse.success("Permissions retrieved successfully", permissions));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/hierarchy")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRoleHierarchy() {
        try {
            Map<String, Object> hierarchy = Map.of(
                    "roles", Arrays.stream(Role.values())
                            .map(role -> Map.of(
                                    "name", role.name(),
                                    "description", role.getDescription(),
                                    "permissions", role.getPermissions().stream()
                                            .map(Enum::name)
                                            .collect(Collectors.toList())
                            ))
                            .collect(Collectors.toList()),
                    "permissions", Arrays.stream(Permission.values())
                            .map(Enum::name)
                            .collect(Collectors.toList())
            );

            return ResponseEntity.ok(ApiResponse.success("Role hierarchy retrieved successfully", hierarchy));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}