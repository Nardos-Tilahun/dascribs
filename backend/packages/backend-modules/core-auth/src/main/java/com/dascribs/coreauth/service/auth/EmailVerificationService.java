package com.dascribs.coreauth.service.auth;

import com.dascribs.coreauth.entity.auth.EmailVerificationToken;
import com.dascribs.coreauth.entity.user.User;
import com.dascribs.coreauth.repository.EmailVerificationTokenRepository;
import com.dascribs.coreauth.repository.UserRepository;
import com.dascribs.coreauth.service.email.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class EmailVerificationService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailVerificationTokenRepository tokenRepository;

    @Autowired
    private EmailService emailService;

    @Value("${app.security.email-verification.token-expiry-hours:24}")
    private int tokenExpiryHours;

    @Value("${app.security.email-verification.max-attempts-per-day:5}")
    private int maxAttemptsPerDay;

    @Value("${app.security.email-verification.resend-cooldown-minutes:2}")
    private int resendCooldownMinutes;

    /**
     * Send initial email verification after registration
     */
    public void sendVerificationEmail(User user) {
        if (user.isEmailVerified()) {
            throw new IllegalStateException("Email is already verified");
        }

        // Rate limiting check
        if (isRateLimited(user.getId())) {
            throw new IllegalStateException("Too many verification attempts. Please try again later.");
        }

        // Check cooldown period
        if (isInCooldownPeriod(user)) {
            throw new IllegalStateException("Please wait before requesting another verification email");
        }

        // Invalidate any existing verification tokens
        tokenRepository.invalidateUserTokens(user.getId(), EmailVerificationToken.TokenType.ACCOUNT_VERIFICATION);

        // Generate secure token
        String token = generateSecureToken();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(tokenExpiryHours);

        // Create and save token
        EmailVerificationToken verificationToken = new EmailVerificationToken(
                user, token, user.getEmail(), EmailVerificationToken.TokenType.ACCOUNT_VERIFICATION, expiresAt
        );
        tokenRepository.save(verificationToken);

        // Update user's verification sent timestamp
        user.setEmailVerificationSentAt(LocalDateTime.now());
        userRepository.save(user);

        // Send verification email
        emailService.sendVerificationEmail(user, token);

        // Clean up expired tokens
        cleanupExpiredTokens();
    }

    /**
     * Verify email with token
     */
    public boolean verifyEmail(String token) {
        // Find valid token
        Optional<EmailVerificationToken> tokenOpt = tokenRepository.findByToken(token);
        if (tokenOpt.isEmpty()) {
            throw new IllegalArgumentException("Invalid or expired verification token");
        }

        EmailVerificationToken verificationToken = tokenOpt.get();

        // Validate token
        if (!verificationToken.isValid()) {
            throw new IllegalArgumentException("Invalid or expired verification token");
        }

        User user = verificationToken.getUser();

        // Verify the email matches
        if (!verificationToken.getEmail().equals(user.getEmail())) {
            throw new IllegalStateException("Email mismatch");
        }

        // Mark email as verified
        user.setEmailVerified(true);
        user.setEmailVerificationSentAt(null);
        userRepository.save(user);

        // Mark token as used
        verificationToken.markAsUsed();
        tokenRepository.save(verificationToken);

        // Send welcome email (optional)
        emailService.sendWelcomeEmail(user, null); // No temp password for verified users

        return true;
    }

    /**
     * Initiate email change process
     */
    public void initiateEmailChange(User user, String newEmail) {
        // Validate new email
        if (newEmail.equals(user.getEmail())) {
            throw new IllegalArgumentException("New email must be different from current email");
        }

        // Check if new email is already taken
        if (userRepository.existsByEmail(newEmail)) {
            throw new IllegalArgumentException("Email is already registered");
        }

        // Rate limiting check
        if (isRateLimited(user.getId())) {
            throw new IllegalStateException("Too many email change attempts. Please try again later.");
        }

        // Invalidate any existing email change tokens
        tokenRepository.invalidateUserTokens(user.getId(), EmailVerificationToken.TokenType.EMAIL_CHANGE_VERIFICATION);

        // Generate secure token
        String token = generateSecureToken();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(tokenExpiryHours);

        // Create and save token
        EmailVerificationToken changeToken = new EmailVerificationToken(
                user, token, newEmail, EmailVerificationToken.TokenType.EMAIL_CHANGE_VERIFICATION, expiresAt
        );
        tokenRepository.save(changeToken);

        // Set pending email
        user.setPendingEmail(newEmail);
        userRepository.save(user);

        // Send email change verification email
        emailService.sendEmailChangeVerification(user, newEmail, token);

        cleanupExpiredTokens();
    }

    /**
     * Complete email change with token
     */
    public boolean completeEmailChange(String token) {
        // Find valid token
        Optional<EmailVerificationToken> tokenOpt = tokenRepository.findByToken(token);
        if (tokenOpt.isEmpty()) {
            throw new IllegalArgumentException("Invalid or expired verification token");
        }

        EmailVerificationToken changeToken = tokenOpt.get();

        // Validate token
        if (!changeToken.isValid()) {
            throw new IllegalArgumentException("Invalid or expired verification token");
        }

        User user = changeToken.getUser();
        String newEmail = changeToken.getEmail();

        // Verify pending email matches
        if (!newEmail.equals(user.getPendingEmail())) {
            throw new IllegalStateException("Email change request mismatch");
        }

        // Check if email is still available
        if (userRepository.existsByEmail(newEmail)) {
            throw new IllegalArgumentException("Email is already registered");
        }

        // Update user email
        String oldEmail = user.getEmail();
        user.setEmail(newEmail);
        user.setPendingEmail(null);
        user.setEmailVerified(true); // New email is verified
        userRepository.save(user);

        // Mark token as used
        changeToken.markAsUsed();
        tokenRepository.save(changeToken);

        // Send notification emails
        emailService.sendEmailChangeNotification(user, oldEmail);
        emailService.sendEmailChangeConfirmation(user, newEmail);

        return true;
    }

    /**
     * Resend verification email
     */
    public void resendVerificationEmail(User user) {
        sendVerificationEmail(user); // Reuse the same logic with cooldown check
    }

    // Helper methods
    private String generateSecureToken() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 32);
    }

    private boolean isRateLimited(Long userId) {
        LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
        long recentAttempts = tokenRepository.countRecentTokensByUserId(userId, oneDayAgo);
        return recentAttempts >= maxAttemptsPerDay;
    }

    private boolean isInCooldownPeriod(User user) {
        if (user.getEmailVerificationSentAt() == null) {
            return false;
        }
        LocalDateTime cooldownUntil = user.getEmailVerificationSentAt().plusMinutes(resendCooldownMinutes);
        return LocalDateTime.now().isBefore(cooldownUntil);
    }

    private void cleanupExpiredTokens() {
        tokenRepository.deleteExpiredTokens(LocalDateTime.now());
    }

    /**
     * Get remaining cooldown time in seconds
     */
    public long getResendCooldownSeconds(User user) {
        if (user.getEmailVerificationSentAt() == null) {
            return 0;
        }
        LocalDateTime cooldownUntil = user.getEmailVerificationSentAt().plusMinutes(resendCooldownMinutes);
        LocalDateTime now = LocalDateTime.now();

        if (now.isBefore(cooldownUntil)) {
            return java.time.Duration.between(now, cooldownUntil).getSeconds();
        }
        return 0;
    }
}