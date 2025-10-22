package com.dascribs.coreauth.service.auth;

import com.dascribs.coreauth.entity.auth.PasswordResetToken;
import com.dascribs.coreauth.entity.user.User;
import com.dascribs.coreauth.repository.PasswordResetTokenRepository;
import com.dascribs.coreauth.repository.UserRepository;
import com.dascribs.coreauth.service.email.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class PasswordResetService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SessionService sessionService;

    @Value("${app.security.password-reset.token-expiry-hours:1}")
    private int tokenExpiryHours;

    @Value("${app.security.password-reset.max-attempts-per-hour:3}")
    private int maxAttemptsPerHour;

    @Value("${app.security.password-reset.min-password-length:8}")
    private int minPasswordLength;

    /**
     * Initiate password reset process
     */
    public void initiatePasswordReset(String email) {
        // Always return success to prevent email enumeration
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isPresent()) {
            User user = userOpt.get();

            // Rate limiting check
            if (isRateLimited(user.getId())) {
                // Still return success, but don't send email
                return;
            }

            // Invalidate any existing tokens for this user
            tokenRepository.invalidateAllUserTokens(user.getId());

            // Generate secure token
            String token = generateSecureToken();
            LocalDateTime expiresAt = LocalDateTime.now().plusHours(tokenExpiryHours);

            // Create and save token
            PasswordResetToken resetToken = new PasswordResetToken(user, token, expiresAt);
            tokenRepository.save(resetToken);

            // Send email (will be logged in dev, actually sent in prod)
            emailService.sendPasswordResetEmail(user, token);
        }

        // Clean up expired tokens
        cleanupExpiredTokens();
    }

    /**
     * Validate and process password reset
     */
    public boolean resetPassword(String token, String newPassword) {
        // Validate password strength
        if (!isPasswordStrong(newPassword)) {
            throw new IllegalArgumentException("Password does not meet security requirements");
        }

        // Find valid token
        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByToken(token);
        if (tokenOpt.isEmpty()) {
            throw new IllegalArgumentException("Invalid or expired reset token");
        }

        PasswordResetToken resetToken = tokenOpt.get();

        // Validate token
        if (!resetToken.isValid()) {
            throw new IllegalArgumentException("Invalid or expired reset token");
        }

        User user = resetToken.getUser();

        // Check if new password is different from current
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new IllegalArgumentException("New password must be different from current password");
        }

        // Update user password
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Mark token as used
        resetToken.markAsUsed();
        tokenRepository.save(resetToken);

        // Logout all existing sessions for security
        sessionService.logoutAllUserSessions(user.getId());

        // Send notification email
        emailService.sendPasswordChangedNotification(user);

        return true;
    }

    /**
     * Validate reset token without using it
     */
    public boolean validateResetToken(String token) {
        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByToken(token);
        return tokenOpt.isPresent() && tokenOpt.get().isValid();
    }

    /**
     * Get user from valid reset token
     */
    public Optional<User> getUserFromValidToken(String token) {
        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByToken(token);
        if (tokenOpt.isPresent() && tokenOpt.get().isValid()) {
            return Optional.of(tokenOpt.get().getUser());
        }
        return Optional.empty();
    }

    // Helper methods
    private String generateSecureToken() {
        return UUID.randomUUID().toString() + "-" + System.currentTimeMillis();
    }

    private boolean isRateLimited(Long userId) {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        long recentAttempts = tokenRepository.countRecentTokensByUserId(userId, oneHourAgo);
        return recentAttempts >= maxAttemptsPerHour;
    }

    private boolean isPasswordStrong(String password) {
        if (password == null || password.length() < minPasswordLength) {
            return false;
        }

        // Check for at least one digit
        if (!password.matches(".*\\d.*")) {
            return false;
        }

        // Check for at least one letter
        return password.matches(".*[a-zA-Z].*");
    }

    private void cleanupExpiredTokens() {
        tokenRepository.deleteExpiredTokens(LocalDateTime.now());
    }
}