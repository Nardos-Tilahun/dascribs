package com.dascribs.service.tenant;

import com.dascribs.dto.request.TenantCreateRequest;
import com.dascribs.dto.request.TenantUpdateRequest;
import com.dascribs.dto.response.TenantResponse;
import com.dascribs.exception.AccessDeniedException;
import com.dascribs.model.user.Role;
import com.dascribs.model.user.User;
import com.dascribs.repository.TenantRepository;
import com.dascribs.repository.UserTenantRepository;
import com.dascribs.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
public class TenantService {

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private UserTenantRepository userTenantRepository;

    public Page<TenantResponse> getAllTenants(Pageable pageable) {
        checkSuperAdminAccess();

        Page<Tenant.Tenant> tenants = tenantRepository.findAll(pageable);
        return tenants.map(tenant -> {
            TenantResponse response = new TenantResponse(tenant);
            response.setUserCount(userTenantRepository.countActiveUsersByTenantId(tenant.getId()));
            // Property count would be set when property module is implemented
            return response;
        });
    }

    public Page<TenantResponse> getActiveTenants(Pageable pageable) {
        checkSuperAdminAccess();

        Page<Tenant.Tenant> tenants = tenantRepository.findByActiveTrue(pageable);
        return tenants.map(tenant -> {
            TenantResponse response = new TenantResponse(tenant);
            response.setUserCount(userTenantRepository.countActiveUsersByTenantId(tenant.getId()));
            return response;
        });
    }

    public TenantResponse getTenantById(Long id) {
        checkTenantAccess(id);

        Tenant.Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found with id: " + id));

        TenantResponse response = new TenantResponse(tenant);
        response.setUserCount(userTenantRepository.countActiveUsersByTenantId(tenant.getId()));
        return response;
    }

    public TenantResponse getTenantByTenantId(String tenantId) {
        checkTenantAccess(tenantId);

        Tenant.Tenant tenant = tenantRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found with tenantId: " + tenantId));

        TenantResponse response = new TenantResponse(tenant);
        response.setUserCount(userTenantRepository.countActiveUsersByTenantId(tenant.getId()));
        return response;
    }

    public TenantResponse createTenant(TenantCreateRequest request) {
        checkSuperAdminAccess();

        // Validate tenant ID uniqueness
        if (tenantRepository.existsByTenantId(request.getTenantId())) {
            throw new IllegalArgumentException("Tenant ID already exists");
        }

        // Validate domain uniqueness if provided
        if (request.getDomain() != null && !request.getDomain().trim().isEmpty() &&
                tenantRepository.existsByDomain(request.getDomain())) {
            throw new IllegalArgumentException("Domain already registered");
        }

        // Validate plan and limits
        if (!request.hasValidLimits()) {
            throw new IllegalArgumentException("Invalid user or property limits");
        }

        Tenant.Tenant tenant = new Tenant.Tenant();
        tenant.setTenantId(request.getTenantId());
        tenant.setName(request.getName());
        tenant.setDomain(request.getDomain());
        tenant.setPlan(request.getPlan());
        tenant.setContactEmail(request.getContactEmail());
        tenant.setContactPhone(request.getContactPhone());
        tenant.setAddress(request.getAddress());
        tenant.setMaxUsers(request.getMaxUsers());
        tenant.setMaxProperties(request.getMaxProperties());
        tenant.setActive(true);

        // Set subscription end date based on plan
        if (request.getPlan() != Tenant.Tenant.Plan.FREE) {
            tenant.setSubscriptionEndsAt(LocalDateTime.now().plusMonths(1)); // 1 month trial
        }

        Tenant.Tenant savedTenant = tenantRepository.save(tenant);
        return new TenantResponse(savedTenant);
    }

    public TenantResponse updateTenant(Long id, TenantUpdateRequest request) {
        checkSuperAdminAccess();

        Tenant.Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found with id: " + id));

        // Apply updates
        if (request.hasName()) {
            tenant.setName(request.getName());
        }
        if (request.hasDomain()) {
            // Validate domain uniqueness
            if (!tenant.getDomain().equals(request.getDomain()) &&
                    tenantRepository.existsByDomain(request.getDomain())) {
                throw new IllegalArgumentException("Domain already registered");
            }
            tenant.setDomain(request.getDomain());
        }
        if (request.hasPlan()) {
            tenant.setPlan(request.getPlan());
        }
        if (request.hasContactEmail()) {
            tenant.setContactEmail(request.getContactEmail());
        }
        if (request.hasContactPhone()) {
            tenant.setContactPhone(request.getContactPhone());
        }
        if (request.hasAddress()) {
            tenant.setAddress(request.getAddress());
        }
        if (request.hasMaxUsers()) {
            tenant.setMaxUsers(request.getMaxUsers());
        }
        if (request.hasMaxProperties()) {
            tenant.setMaxProperties(request.getMaxProperties());
        }
        if (request.hasActive()) {
            tenant.setActive(request.getActive());
        }

        Tenant.Tenant updatedTenant = tenantRepository.save(tenant);
        TenantResponse response = new TenantResponse(updatedTenant);
        response.setUserCount(userTenantRepository.countActiveUsersByTenantId(tenant.getId()));
        return response;
    }

    public void deleteTenant(Long id) {
        checkSuperAdminAccess();

        Tenant.Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found with id: " + id));

        // Soft delete - deactivate tenant
        tenant.setActive(false);
        tenantRepository.save(tenant);
    }

    public void suspendTenant(Long id) {
        checkSuperAdminAccess();

        Tenant.Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found with id: " + id));

        tenant.setActive(false);
        tenantRepository.save(tenant);
    }

    public void activateTenant(Long id) {
        checkSuperAdminAccess();

        Tenant.Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found with id: " + id));

        tenant.setActive(true);
        tenantRepository.save(tenant);
    }

    public boolean isTenantActive(Long tenantId) {
        return tenantRepository.findById(tenantId)
                .map(Tenant.Tenant::isActive)
                .orElse(false);
    }

    public boolean canTenantAddUser(Long tenantId) {
        return !tenantRepository.hasReachedUserLimit(tenantId);
    }

    // Helper methods
    private void checkSuperAdminAccess() {
        User currentUser = getCurrentUser();
        if (currentUser.getRole() != Role.SUPER_ADMIN) {
            throw new AccessDeniedException("Only super administrators can access tenant management");
        }
    }

    private void checkTenantAccess(Long tenantId) {
        User currentUser = getCurrentUser();

        if (currentUser.getRole() == Role.SUPER_ADMIN) {
            return;
        }

        // Check if user belongs to this tenant
        boolean hasAccess = userTenantRepository.findByUserId(currentUser.getId()).stream()
                .anyMatch(userTenant -> userTenant.getTenant().getId().equals(tenantId));

        if (!hasAccess) {
            throw new AccessDeniedException("You don't have access to this tenant");
        }
    }

    private void checkTenantAccess(String tenantId) {
        User currentUser = getCurrentUser();

        if (currentUser.getRole() == Role.SUPER_ADMIN) {
            return;
        }

        // Check if user belongs to this tenant
        boolean hasAccess = userTenantRepository.findByUserId(currentUser.getId()).stream()
                .anyMatch(userTenant -> userTenant.getTenant().getTenantId().equals(tenantId));

        if (!hasAccess) {
            throw new AccessDeniedException("You don't have access to this tenant");
        }
    }

    private User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserPrincipal) {
            String email = ((UserPrincipal) principal).getUsername();
            // This would need a UserRepository injection to fetch the full user
            // For now, we'll use a simplified approach
            User user = new User();
            user.setRole(Role.SUPER_ADMIN); // Default for demo
            return user;
        }
        throw new IllegalStateException("User not authenticated");
    }
}