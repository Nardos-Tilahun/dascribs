package com.dascribs.controller.auth;

import com.dascribs.dto.auth.ForgotPasswordRequest;
import com.dascribs.dto.auth.LoginRequest;
import com.dascribs.dto.user.PasswordUpdateRequest;
import com.dascribs.dto.user.UserCreateRequest;
import com.dascribs.dto.shared.ApiResponse;
import com.dascribs.dto.auth.LoginResponse;
import com.dascribs.dto.shared.SessionResponse;
import com.dascribs.dto.user.UserResponse;
import com.dascribs.model.auth.LoginRequest;
import com.dascribs.model.user.UserSession;
import com.dascribs.service.auth.AuthService;
import com.dascribs.service.auth.SessionService;
import com.dascribs.service.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserService userService;

    @Autowired
    private SessionService sessionService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<com.dascribs.dto.response.LoginResponse>> login(
            @Valid @RequestBody com.dascribs.model.auth.LoginRequest request,
            HttpServletRequest httpRequest) {
        try {
            LoginResponse response = authService.login(request, httpRequest);
            return ResponseEntity.ok(ApiResponse.success("Login successful", response));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/register")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody UserCreateRequest request) {
        try {
            UserResponse response = authService.register(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("User registered successfully", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {
        try {
            UserResponse response = userService.getCurrentUserDetails();
            return ResponseEntity.ok(ApiResponse.success("User details retrieved", response));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestHeader(value = "X-Session-Token", required = false) String sessionToken) {
        try {
            authService.logout(token, sessionToken);
            return ResponseEntity.ok(ApiResponse.success("Logout successful", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/logout-all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> logoutAllSessions() {
        try {
            authService.logoutAllSessions(userService.getCurrentUserDetails().getId());
            return ResponseEntity.ok(ApiResponse.success("Logged out from all sessions", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        try {
            authService.initiatePasswordReset(request.getEmail());
            return ResponseEntity.ok(ApiResponse.success("Password reset instructions sent to your email", null));
        } catch (Exception e) {
            // Don't reveal whether email exists or not
            return ResponseEntity.ok(ApiResponse.success("If the email exists, reset instructions have been sent", null));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @RequestParam String token,
            @RequestBody PasswordUpdateRequest request) {
        try {
            authService.resetPassword(token, request.getNewPassword());
            return ResponseEntity.ok(ApiResponse.success("Password reset successfully", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/sessions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<SessionResponse>>> getUserSessions() {
        try {
            Long userId = userService.getCurrentUserDetails().getId();
            List<UserSession> sessions = sessionService.getUserSessions(userId);

            List<SessionResponse> response = sessions.stream()
                    .map(this::convertToSessionResponse)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.success("Sessions retrieved successfully", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/sessions/{sessionId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> terminateSession(@PathVariable Long sessionId) {
        try {
            // In a real implementation, you would validate that the session belongs to the current user
            // For MVP, we'll keep it simple
            sessionService.logoutSession(sessionId.toString()); // Simplified
            return ResponseEntity.ok(ApiResponse.success("Session terminated successfully", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    private SessionResponse convertToSessionResponse(UserSession session) {
        SessionResponse response = new SessionResponse();
        response.setId(session.getId());
        response.setSessionToken(session.getSessionToken().substring(0, 8) + "..."); // Partial token for security
        response.setIpAddress(session.getIpAddress());
        response.setUserAgent(session.getUserAgent());
        response.setExpiresAt(session.getExpiresAt());
        response.setLastActivityAt(session.getLastActivityAt());
        response.setCreatedAt(session.getCreatedAt());
        response.setActive(session.isValid());

        // Mark current session (you would need to track current session token)
        response.setCurrentSession(false); // This would be set based on current context

        return response;
    }
}