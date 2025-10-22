package com.dascribs.coreauth.controller.AuthAndUSer.tenant;


import com.dascribs.coreauth.dto.shared.ApiResponse;
import com.dascribs.coreauth.dto.shared.PaginatedResponse;
import com.dascribs.coreauth.dto.tenant.TenantCreateRequest;
import com.dascribs.coreauth.dto.tenant.TenantResponse;
import com.dascribs.coreauth.dto.tenant.TenantUpdateRequest;
import com.dascribs.coreauth.service.tenant.TenantService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/tenants")
public class TenantController {

    @Autowired
    private TenantService tenantService;

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<PaginatedResponse<TenantResponse>>> getAllTenants(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        try {
            Sort sort = sortDirection.equalsIgnoreCase("desc") ?
                    Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
            Pageable pageable = PageRequest.of(page, size, sort);

            Page<TenantResponse> tenantsPage = tenantService.getAllTenants(pageable);

            PaginatedResponse<TenantResponse> paginatedResponse = new PaginatedResponse<>(
                    tenantsPage.getContent(),
                    tenantsPage.getNumber(),
                    tenantsPage.getSize(),
                    tenantsPage.getTotalElements(),
                    tenantsPage.getTotalPages()
            );

            return ResponseEntity.ok(ApiResponse.success("Tenants retrieved successfully", paginatedResponse));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/active")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<PaginatedResponse<TenantResponse>>> getActiveTenants(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
            Page<TenantResponse> tenantsPage = tenantService.getActiveTenants(pageable);

            PaginatedResponse<TenantResponse> paginatedResponse = new PaginatedResponse<>(
                    tenantsPage.getContent(),
                    tenantsPage.getNumber(),
                    tenantsPage.getSize(),
                    tenantsPage.getTotalElements(),
                    tenantsPage.getTotalPages()
            );

            return ResponseEntity.ok(ApiResponse.success("Active tenants retrieved successfully", paginatedResponse));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or @tenantService.isTenantActive(#id)")
    public ResponseEntity<ApiResponse<TenantResponse>> getTenantById(@PathVariable Long id) {
        try {
            TenantResponse tenant = tenantService.getTenantById(id);
            return ResponseEntity.ok(ApiResponse.success("Tenant retrieved successfully", tenant));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/tenant-id/{tenantId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<TenantResponse>> getTenantByTenantId(@PathVariable String tenantId) {
        try {
            TenantResponse tenant = tenantService.getTenantByTenantId(tenantId);
            return ResponseEntity.ok(ApiResponse.success("Tenant retrieved successfully", tenant));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<TenantResponse>> createTenant(@Valid @RequestBody TenantCreateRequest request) {
        try {
            TenantResponse tenant = tenantService.createTenant(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Tenant created successfully", tenant));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<TenantResponse>> updateTenant(
            @PathVariable Long id,
            @Valid @RequestBody TenantUpdateRequest request) {

        try {
            TenantResponse tenant = tenantService.updateTenant(id, request);
            return ResponseEntity.ok(ApiResponse.success("Tenant updated successfully", tenant));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteTenant(@PathVariable Long id) {
        try {
            tenantService.deleteTenant(id);
            return ResponseEntity.ok(ApiResponse.success("Tenant deleted successfully", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/suspend")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> suspendTenant(@PathVariable Long id) {
        try {
            tenantService.suspendTenant(id);
            return ResponseEntity.ok(ApiResponse.success("Tenant suspended successfully", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> activateTenant(@PathVariable Long id) {
        try {
            tenantService.activateTenant(id);
            return ResponseEntity.ok(ApiResponse.success("Tenant activated successfully", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{id}/stats")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTenantStats(@PathVariable Long id) {
        try {
            TenantResponse tenant = tenantService.getTenantById(id);

            Map<String, Object> stats = Map.of(
                    "tenant", tenant,
                    "userLimitReached", tenant.hasReachedUserLimit(),
//                    "propertyLimitReached", tenant.hasReachedPropertyLimit(),
                    "subscriptionActive", tenant.isSubscriptionActive(),
                    "daysUntilExpiry", tenant.getSubscriptionEndsAt() != null ?
                            java.time.Duration.between(java.time.LocalDateTime.now(), tenant.getSubscriptionEndsAt()).toDays() : -1
            );

            return ResponseEntity.ok(ApiResponse.success("Tenant stats retrieved successfully", stats));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}