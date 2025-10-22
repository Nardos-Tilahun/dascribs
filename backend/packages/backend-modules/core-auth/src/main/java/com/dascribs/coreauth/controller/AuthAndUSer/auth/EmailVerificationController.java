package com.dascribs.coreauth.controller.AuthAndUSer.auth;

import com.dascribs.coreauth.dto.shared.ApiResponse;
import com.dascribs.coreauth.dto.user.EmailChangeRequest;
import com.dascribs.coreauth.entity.user.User;
import com.dascribs.coreauth.service.auth.EmailVerificationService;
import com.dascribs.coreauth.service.user.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth/email")
public class EmailVerificationController {

    @Autowired
    private EmailVerificationService emailVerificationService;

    @Autowired
    private UserService userService;

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<String>> verifyEmail(@RequestParam String token) {
        try {
            boolean success = emailVerificationService.verifyEmail(token);

            if (success) {
                return ResponseEntity.ok(ApiResponse.success("Email verified successfully! You can now log in."));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Failed to verify email"));
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An error occurred during email verification"));
        }
    }

    @PostMapping("/resend-verification")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<String>> resendVerificationEmail() {
        try {
            User currentUser = userService.getCurrentUser();
            emailVerificationService.resendVerificationEmail(currentUser);

            return ResponseEntity.ok(ApiResponse.success("Verification email sent successfully"));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to send verification email"));
        }
    }

    @GetMapping("/resend-cooldown")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Long>> getResendCooldown() {
        try {
            User currentUser = userService.getCurrentUser();
            long cooldownSeconds = emailVerificationService.getResendCooldownSeconds(currentUser);

            return ResponseEntity.ok(ApiResponse.success("Cooldown retrieved", cooldownSeconds));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to get cooldown information"));
        }
    }

    @PostMapping("/change")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<String>> initiateEmailChange(@Valid @RequestBody EmailChangeRequest request) {
        try {
            User currentUser = userService.getCurrentUser();
            emailVerificationService.initiateEmailChange(currentUser, request.getNewEmail());

            return ResponseEntity.ok(ApiResponse.success("Verification email sent to your new email address"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to initiate email change"));
        }
    }

    @PostMapping("/change/confirm")
    public ResponseEntity<ApiResponse<String>> completeEmailChange(@RequestParam String token) {
        try {
            boolean success = emailVerificationService.completeEmailChange(token);

            if (success) {
                return ResponseEntity.ok(ApiResponse.success("Email changed successfully!"));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Failed to change email"));
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An error occurred during email change"));
        }
    }

    @GetMapping("/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getEmailStatus() {
        try {
            User currentUser = userService.getCurrentUser();

            Map<String, Object> status = Map.of(
                    "email", currentUser.getEmail(),
                    "verified", currentUser.isEmailVerified(),
                    "pendingEmail", currentUser.getPendingEmail(),
                    "verificationSent", currentUser.getEmailVerificationSentAt() != null,
                    "canResend", emailVerificationService.getResendCooldownSeconds(currentUser) == 0,
                    "cooldownSeconds", emailVerificationService.getResendCooldownSeconds(currentUser)
            );

            return ResponseEntity.ok(ApiResponse.success("Email status retrieved", status));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to get email status"));
        }
    }
}