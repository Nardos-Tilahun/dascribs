package com.dascribs.coreauth.service.auth;

import com.dascribs.coreauth.dto.auth.LoginRequest;
import com.dascribs.coreauth.dto.auth.LoginResponse;
import com.dascribs.coreauth.dto.user.UserCreateRequest;
import com.dascribs.coreauth.dto.user.UserResponse;
import com.dascribs.shared.exception.UserNotFoundException;
import com.dascribs.coreauth.entity.tenant.Tenant;
import com.dascribs.coreauth.entity.user.Role;
import com.dascribs.coreauth.entity.user.User;
import com.dascribs.coreauth.entity.user.UserTenant;
import com.dascribs.coreauth.repository.TenantRepository;
import com.dascribs.coreauth.repository.UserRepository;
import com.dascribs.coreauth.repository.UserTenantRepository;
import com.dascribs.coreauth.security.CustomUserDetailsService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Transactional
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private UserTenantRepository userTenantRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private EmailVerificationService emailVerificationService;

    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        try {
            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Get user details
            CustomUserDetailsService.UserPrincipal userPrincipal = (CustomUserDetailsService.UserPrincipal) authentication.getPrincipal();
            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new UserNotFoundException("User not found"));

            if (!user.isEmailVerified()) {
                throw new RuntimeException("Please verify your email address before logging in. Check your inbox for verification instructions.");
            }

            // Update last login
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);

            // Generate JWT token
            String jwtToken = jwtService.generateToken(userPrincipal);
            LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(jwtService.getJwtExpiration() / 1000);

            // Create session
            String ipAddress = getClientIpAddress(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");
            var session = sessionService.createSession(user, ipAddress, userAgent);

            // Get tenant information
            Optional<UserTenant> primaryTenant = userTenantRepository.findPrimaryTenantByUserId(user.getId());

            LoginResponse response = new LoginResponse(
                    jwtToken,
                    user.getId(),
                    user.getEmail(),
                    user.getFullName(),
                    user.getRole(),
                    expiresAt
            );

            response.setSessionToken(session.getSessionToken());

            if (primaryTenant.isPresent()) {
                response.setTenantId(primaryTenant.get().getTenant().getId());
                response.setTenantName(primaryTenant.get().getTenant().getName());
            }

            return response;

        } catch (BadCredentialsException e) {
            throw new BadCredentialsException("Invalid email or password");
        }
    }

    public UserResponse register(UserCreateRequest request) {
        // Validate email uniqueness
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }

        // Validate role-specific requirements
        if (!request.isValidForRole()) {
            throw new IllegalArgumentException("Invalid role configuration");
        }

        // For non-super-admin registrations, ensure they're associated with a tenant
        Tenant tenant = null;
        if (request.getRole() != Role.SUPER_ADMIN) {
            tenant = getOrCreateDefaultTenant();

            // Check user limit for tenant
           // if (tenantRepository.hasReachedUserLimit(tenant.getId())) {
           //     throw new IllegalArgumentException("Tenant user limit reached. Please upgrade your plan.");
           // }
        }

        // Create user
        User user = createUserFromRequest(request);

        User savedUser = userRepository.save(user);

        // Associate user with tenant if applicable
        if (tenant != null) {
            UserTenant userTenant = new UserTenant(savedUser, tenant, true);
            userTenantRepository.save(userTenant);
        }
        emailVerificationService.sendVerificationEmail(savedUser);
        return new UserResponse(savedUser);
    }

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("User not authenticated");
        }

        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    public void logout(String token, String sessionToken) {
        // Invalidate session if provided
        if (sessionToken != null && !sessionToken.trim().isEmpty()) {
            sessionService.logoutSession(sessionToken);
        }
        // JWT tokens are stateless, so we just rely on expiration
    }

    public void logoutAllSessions(Long userId) {
        sessionService.logoutAllUserSessions(userId);
    }

    public boolean validatePassword(Long userId, String password) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        return passwordEncoder.matches(password, user.getPassword());
    }

    public void changePassword(Long userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Logout all sessions for security
        logoutAllSessions(userId);
    }

    public void initiatePasswordReset(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));

        // In a real implementation, you would:
        // 1. Generate reset token
        // 2. Save token with expiry
        // 3. Send email with reset link
        // For MVP, we'll just log this action
        System.out.println("Password reset initiated for user: " + email);
    }

    public void resetPassword(String token, String newPassword) {
        // In a real implementation, you would:
        // 1. Validate reset token
        // 2. Find user by token
        // 3. Update password
        // 4. Invalidate token
        // 5. Logout all sessions
        // For MVP, we'll skip token validation
        System.out.println("Password reset with token: " + token);
    }

    private User createUserFromRequest(UserCreateRequest request) {
        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone());
        user.setRole(request.getRole());
        user.setSalary(request.getSalary());
        user.setCommissionRate(request.getCommissionRate());
        user.setActive(true);
        user.setEmailVerified(false); // Email verification would be sent

        return user;
    }

    private Tenant getOrCreateDefaultTenant() {
        // In a real application, you'd get the tenant from the request context
        // For demo purposes, we'll use or create a default tenant
        Optional<Tenant> existingTenant = tenantRepository.findByTenantId("default_tenant");
        if (existingTenant.isPresent()) {
            return existingTenant.get();
        }

        Tenant defaultTenant = new Tenant();
        defaultTenant.setTenantId("default_tenant");
        defaultTenant.setName("Default Organization");
        defaultTenant.setPlan(Tenant.Plan.FREE);
        defaultTenant.setActive(true);

        return tenantRepository.save(defaultTenant);
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null) {
            return xfHeader.split(",")[0];
        }
        return request.getRemoteAddr();
    }
}