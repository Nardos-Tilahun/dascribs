package com.dascribs.coreauth.controller.AuthAndUSer.auth;

import com.dascribs.coreauth.dto.user.ForgotPasswordRequest;
import com.dascribs.coreauth.dto.user.PasswordUpdateRequest;
import com.dascribs.coreauth.dto.shared.ApiResponse;
import com.dascribs.coreauth.dto.user.UserResponse;
import com.dascribs.coreauth.entity.user.User;
import com.dascribs.coreauth.service.auth.PasswordResetService;
import com.dascribs.coreauth.service.user.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth/password")
public class PasswordController {

    @Autowired
    private PasswordResetService passwordResetService;

    @Autowired
    private UserService userService;

    @PostMapping("/forgot")
    public ResponseEntity<ApiResponse<String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        try {
            passwordResetService.initiatePasswordReset(request.getEmail());

            // Always return success to prevent email enumeration
            return ResponseEntity.ok(ApiResponse.success(
                    "If the email exists, password reset instructions have been sent"
            ));
        } catch (Exception e) {
            // Still return success for security
            return ResponseEntity.ok(ApiResponse.success(
                    "If the email exists, password reset instructions have been sent"
            ));
        }
    }

    @GetMapping("/validate-reset-token")
    public ResponseEntity<ApiResponse<Boolean>> validateResetToken(@RequestParam String token) {
        try {
            boolean isValid = passwordResetService.validateResetToken(token);
            return ResponseEntity.ok(ApiResponse.success("Token validation completed", isValid));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.success("Token validation completed", false));
        }
    }

    @PostMapping("/reset")
    public ResponseEntity<ApiResponse<String>> resetPassword(@Valid @RequestBody PasswordUpdateRequest.ResetPasswordRequest request) {
        try {
            boolean success = passwordResetService.resetPassword(request.getToken(), request.getNewPassword());

            if (success) {
                return ResponseEntity.ok(ApiResponse.success("Password reset successfully"));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Failed to reset password"));
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An error occurred during password reset"));
        }
    }

    @GetMapping("/reset/user-info")
    public ResponseEntity<ApiResponse<UserResponse>> getUserInfoFromToken(@RequestParam String token) {
        try {
            Optional<User> userOpt = passwordResetService.getUserFromValidToken(token);

            if (userOpt.isPresent()) {
                User user = userOpt.get();
                UserResponse response = new UserResponse(user);
                return ResponseEntity.ok(ApiResponse.success("User info retrieved", response));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Invalid or expired token"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Unable to retrieve user information"));
        }
    }
}