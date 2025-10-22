package com.dascribs.coreauth.service.user;


import com.dascribs.coreauth.dto.user.UserCreateRequest;
import com.dascribs.coreauth.dto.user.UserResponse;
import com.dascribs.coreauth.dto.user.UserUpdateRequest;

import com.dascribs.coreauth.entity.tenant.Tenant;
import com.dascribs.coreauth.entity.user.Role;
import com.dascribs.coreauth.entity.user.User;
import com.dascribs.coreauth.entity.user.UserTenant;
import com.dascribs.coreauth.repository.TenantRepository;
import com.dascribs.coreauth.repository.UserRepository;
import com.dascribs.coreauth.repository.UserTenantRepository;
import com.dascribs.coreauth.security.UserPrincipal;
import com.dascribs.shared.exception.AccessDeniedException;
import com.dascribs.shared.exception.UserNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private UserTenantRepository userTenantRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public Page<UserResponse> getAllUsers(Pageable pageable) {
        User currentUser = getCurrentUser();

        // Apply tenant filtering for non-super-admins
        if (currentUser.getRole() == Role.SUPER_ADMIN) {
            return userRepository.findAll(pageable).map(this::enrichUserResponse);
        } else {
            // Get user's tenants and filter by them
            Optional<UserTenant> primaryTenant = userTenantRepository.findPrimaryTenantByUserId(currentUser.getId());
            if (primaryTenant.isPresent()) {
                Long tenantId = primaryTenant.get().getTenant().getId();
                return userRepository.findByTenantId(tenantId, pageable).map(this::enrichUserResponse);
            } else {
                throw new AccessDeniedException("User is not associated with any tenant");
            }
        }
    }

    public Page<UserResponse> getActiveUsers(Pageable pageable) {
        User currentUser = getCurrentUser();

        if (currentUser.getRole() == Role.SUPER_ADMIN) {
            return userRepository.findByActiveTrue(pageable).map(this::enrichUserResponse);
        } else {
            Optional<UserTenant> primaryTenant = userTenantRepository.findPrimaryTenantByUserId(currentUser.getId());
            if (primaryTenant.isPresent()) {
                Long tenantId = primaryTenant.get().getTenant().getId();
                return userRepository.findByTenantIdAndActive(tenantId, true, pageable).map(this::enrichUserResponse);
            } else {
                throw new AccessDeniedException("User is not associated with any tenant");
            }
        }
    }

    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        // Check permissions
        checkUserAccessPermission(user);

        return enrichUserResponse(user);
    }

    public UserResponse getCurrentUserDetails() {
        User currentUser = getCurrentUser();
        return enrichUserResponse(currentUser);
    }

    public UserResponse createUser(UserCreateRequest request) {
        User currentUser = getCurrentUser();

        // Check permissions
        if (!canCreateRole(currentUser, request.getRole())) {
            throw new AccessDeniedException("You don't have permission to create users with role: " + request.getRole());
        }

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        // Validate role-specific requirements
        if (!request.isValidForRole()) {
            throw new IllegalArgumentException("Invalid role configuration");
        }

        // For non-super-admins, associate with current tenant
        Tenant tenant = null;
        if (currentUser.getRole() != Role.SUPER_ADMIN) {
            tenant = getCurrentUserTenant(currentUser);

            // Check tenant user limits
            // if (tenantRepository.hasReachedUserLimit(tenant.getId())) {
            //    throw new IllegalArgumentException("Tenant user limit reached. Please upgrade your plan.");
            // }
        }

        // Create user
        User user = createUserFromRequest(request, currentUser);
        User savedUser = userRepository.save(user);

        // Associate with tenant if applicable
        if (tenant != null) {
            UserTenant userTenant = new UserTenant(savedUser, tenant, false); // Not primary
            userTenantRepository.save(userTenant);
        }

        return enrichUserResponse(savedUser);
    }

    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        User currentUser = getCurrentUser();

        // Check permissions
        if (!canModifyUser(currentUser, user)) {
            throw new AccessDeniedException("You don't have permission to modify this user");
        }

        // Check if email is changed and already exists
        if (request.hasEmail() && !user.getEmail().equals(request.getEmail()) &&
                userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        // Apply updates
        if (request.hasFullName()) {
            user.setFullName(request.getFullName());
        }
        if (request.hasEmail()) {
            user.setEmail(request.getEmail());
        }
        if (request.hasPhone()) {
            user.setPhone(request.getPhone());
        }
        if (request.hasRole()) {
            // Check role modification permissions
            if (!canModifyRole(currentUser, request.getRole())) {
                throw new AccessDeniedException("You don't have permission to assign role: " + request.getRole());
            }
            user.setRole(request.getRole());
        }
        if (request.hasSalary()) {
            user.setSalary(request.getSalary());
        }
        if (request.hasCommissionRate()) {
            user.setCommissionRate(request.getCommissionRate());
        }
        if (request.hasActive()) {
            // Check if user can deactivate this user
            if (!canModifyUserStatus(currentUser, user)) {
                throw new AccessDeniedException("You don't have permission to modify this user's status");
            }
            user.setActive(request.getActive());
        }

        User updatedUser = userRepository.save(user);
        return enrichUserResponse(updatedUser);
    }

    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        User currentUser = getCurrentUser();

        // Check permissions
        if (!canModifyUser(currentUser, user)) {
            throw new AccessDeniedException("You don't have permission to delete this user");
        }

        // Prevent self-deletion
        if (user.getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("You cannot delete your own account");
        }

        // Soft delete - deactivate user
        user.setActive(false);
        userRepository.save(user);
    }

    public List<UserResponse> getUsersByRole(Role role) {
        User currentUser = getCurrentUser();

        if (currentUser.getRole() == Role.SUPER_ADMIN) {
            return userRepository.findByRole(role).stream()
                    .map(this::enrichUserResponse)
                    .collect(Collectors.toList());
        } else {
            Optional<UserTenant> primaryTenant = userTenantRepository.findPrimaryTenantByUserId(currentUser.getId());
            if (primaryTenant.isPresent()) {
                Long tenantId = primaryTenant.get().getTenant().getId();
                return userRepository.findByTenantIdAndRole(tenantId, role).stream()
                        .map(this::enrichUserResponse)
                        .collect(Collectors.toList());
            } else {
                throw new AccessDeniedException("User is not associated with any tenant");
            }
        }
    }

    public void updateUserPassword(Long id, String currentPassword, String newPassword) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        User currentUser = getCurrentUser();

        // Users can only change their own password, or admins can change any (with current password)
        if (!currentUser.getId().equals(id)) {
            if (currentUser.getRole() != Role.SUPER_ADMIN && currentUser.getRole() != Role.ADMIN) {
                throw new AccessDeniedException("You can only change your own password");
            }
            // For admin changing another user's password, we might skip current password validation
            // For security, we still require it
        }

        // Validate current password
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public void updateUserProfileImage(Long id, String imageUrl) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        User currentUser = getCurrentUser();

        // Users can only update their own profile image
        if (!currentUser.getId().equals(id) &&
                currentUser.getRole() != Role.SUPER_ADMIN &&
                currentUser.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("You can only update your own profile image");
        }

        user.setProfileImageUrl(imageUrl);
        userRepository.save(user);
    }

    // Helper methods
    public User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserPrincipal) {
            String email = ((UserPrincipal) principal).getUsername();
            return userRepository.findByEmail(email)
                    .orElseThrow(() -> new UserNotFoundException("Current user not found"));
        }
        throw new IllegalStateException("User not authenticated");
    }

    private UserResponse enrichUserResponse(User user) {
        UserResponse response = new UserResponse(user);

        // Add tenant information
        Optional<UserTenant> primaryTenant = userTenantRepository.findPrimaryTenantByUserId(user.getId());
        if (primaryTenant.isPresent()) {
            response.setTenantId(primaryTenant.get().getTenant().getId());
            response.setTenantName(primaryTenant.get().getTenant().getName());
        }

        return response;
    }

    private User createUserFromRequest(UserCreateRequest request, User createdBy) {
        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone());
        user.setRole(request.getRole());
        user.setSalary(request.getSalary());
        user.setCommissionRate(request.getCommissionRate());
        user.setActive(true);
        user.setEmailVerified(false);

        return user;
    }

    private Tenant getCurrentUserTenant(User user) {
        return userTenantRepository.findPrimaryTenantByUserId(user.getId())
                .orElseThrow(() -> new AccessDeniedException("User is not associated with any tenant"))
                .getTenant();
    }

    // Permission checking methods
    private boolean canCreateRole(User currentUser, Role targetRole) {
        if (currentUser.getRole() == Role.SUPER_ADMIN) {
            return true;
        }

        if (currentUser.getRole() == Role.ADMIN) {
            return targetRole == Role.AGENT || targetRole == Role.CLIENT;
        }

        return false;
    }

    private boolean canModifyUser(User currentUser, User targetUser) {
        if (currentUser.getRole() == Role.SUPER_ADMIN) {
            return true;
        }

        if (currentUser.getRole() == Role.ADMIN) {
            // Admin can modify AGENT and CLIENT in their tenant
            if (targetUser.getRole() == Role.AGENT || targetUser.getRole() == Role.CLIENT) {
                return areUsersInSameTenant(currentUser, targetUser);
            }
            return false;
        }

        // Users can only modify themselves
        return currentUser.getId().equals(targetUser.getId());
    }

    private boolean canModifyRole(User currentUser, Role targetRole) {
        if (currentUser.getRole() == Role.SUPER_ADMIN) {
            return true;
        }

        if (currentUser.getRole() == Role.ADMIN) {
            return targetRole == Role.AGENT || targetRole == Role.CLIENT;
        }

        return false;
    }

    private boolean canModifyUserStatus(User currentUser, User targetUser) {
        // Prevent users from deactivating themselves
        if (currentUser.getId().equals(targetUser.getId())) {
            return false;
        }

        return canModifyUser(currentUser, targetUser);
    }

    private boolean areUsersInSameTenant(User user1, User user2) {
        Optional<UserTenant> tenant1 = userTenantRepository.findPrimaryTenantByUserId(user1.getId());
        Optional<UserTenant> tenant2 = userTenantRepository.findPrimaryTenantByUserId(user2.getId());

        return tenant1.isPresent() && tenant2.isPresent() &&
                tenant1.get().getTenant().getId().equals(tenant2.get().getTenant().getId());
    }

    private void checkUserAccessPermission(User targetUser) {
        User currentUser = getCurrentUser();

        if (currentUser.getRole() == Role.SUPER_ADMIN) {
            return;
        }

        if (currentUser.getRole() == Role.ADMIN) {
            if (!areUsersInSameTenant(currentUser, targetUser)) {
                throw new AccessDeniedException("You can only access users in your tenant");
            }
            return;
        }

        // Regular users can only access their own data
        if (!currentUser.getId().equals(targetUser.getId())) {
            throw new AccessDeniedException("You can only access your own data");
        }
    }
}