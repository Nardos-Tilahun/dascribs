package com.dascribs.coreauth.service.email;

import com.dascribs.coreauth.entity.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import jakarta.mail.internet.MimeMessage;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Main interface
public interface EmailService {
    void sendPasswordResetEmail(User user, String resetToken);
    void sendPasswordChangedNotification(User user);
    void sendWelcomeEmail(User user, String temporaryPassword);
    void sendVerificationEmail(User user, String verificationToken);
    void sendEmailChangeVerification(User user, String newEmail, String verificationToken);
    void sendEmailChangeNotification(User user, String oldEmail);
    void sendEmailChangeConfirmation(User user, String newEmail);
}

// ==================== PRODUCTION ====================
@Component
@Profile("production")
class ProductionEmailService implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(ProductionEmailService.class);

    @Value("${app.email.from}")
    private String fromEmail;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Override
    public void sendPasswordResetEmail(User user, String resetToken) {
        if (mailSender == null) {
            logger.error("JavaMailSender not configured for production! Check your SMTP settings.");
            return;
        }

        try {
            String resetLink = frontendUrl + "/reset-password?token=" + resetToken;

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(user.getEmail());
            helper.setFrom(fromEmail, "Dascribs");
            helper.setSubject("Password Reset Request - Dascribs");

            String emailContent = buildPasswordResetEmail(user.getFullName(), resetLink);
            helper.setText(emailContent, true);

            mailSender.send(message);

            logger.info("✅ Password reset email sent to: {}", user.getEmail());

        } catch (Exception e) {
            logger.error("❌ Failed to send password reset email to {}: {}", user.getEmail(), e.getMessage());
        }
    }

    @Override
    public void sendPasswordChangedNotification(User user) {
        if (mailSender == null) {
            logger.error("JavaMailSender not configured for production!");
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(user.getEmail());
            helper.setFrom(fromEmail, "DaScribs");
            helper.setSubject("Password Changed - DaScribs");

            String emailContent = buildPasswordChangedEmail(user.getFullName());
            helper.setText(emailContent, true);

            mailSender.send(message);

            logger.info("✅ Password change notification sent to: {}", user.getEmail());

        } catch (Exception e) {
            logger.error("❌ Failed to send password change notification to {}: {}", user.getEmail(), e.getMessage());
        }
    }

    @Override
    public void sendWelcomeEmail(User user, String temporaryPassword) {
        if (mailSender == null) {
            logger.error("JavaMailSender not configured for production!");
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(user.getEmail());
            helper.setFrom(fromEmail, "DaScribs");
            helper.setSubject("Welcome to DaScribs!");

            String emailContent = buildWelcomeEmail(user.getFullName(), user.getEmail(), temporaryPassword);
            helper.setText(emailContent, true);

            mailSender.send(message);

            logger.info("✅ Welcome email sent to: {}", user.getEmail());

        } catch (Exception e) {
            logger.error("❌ Failed to send welcome email to {}: {}", user.getEmail(), e.getMessage());
        }
    }

    @Override
    public void sendVerificationEmail(User user, String verificationToken) {
        if (mailSender == null) {
            logger.error("JavaMailSender not configured for production!");
            return;
        }

        try {
            String verificationLink = frontendUrl + "/verify-email?token=" + verificationToken;

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(user.getEmail());
            helper.setFrom(fromEmail, "DaScribs");
            helper.setSubject("Verify Your Email - DaScribs");

            String emailContent = buildVerificationEmail(user.getFullName(), verificationLink);
            helper.setText(emailContent, true);

            mailSender.send(message);

            logger.info("✅ Verification email sent to: {}", user.getEmail());

        } catch (Exception e) {
            logger.error("❌ Failed to send verification email to {}: {}", user.getEmail(), e.getMessage());
        }
    }

    @Override
    public void sendEmailChangeVerification(User user, String newEmail, String verificationToken) {
        if (mailSender == null) {
            logger.error("JavaMailSender not configured for production!");
            return;
        }

        try {
            String verificationLink = frontendUrl + "/verify-email-change?token=" + verificationToken;

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(newEmail);
            helper.setFrom(fromEmail, "DaScribs");
            helper.setSubject("Confirm Your Email Change - DaScribs");

            String emailContent = buildEmailChangeVerificationEmail(user.getFullName(), newEmail, verificationLink);
            helper.setText(emailContent, true);

            mailSender.send(message);

            logger.info("✅ Email change verification sent to: {}", newEmail);

        } catch (Exception e) {
            logger.error("❌ Failed to send email change verification to {}: {}", newEmail, e.getMessage());
        }
    }

    @Override
    public void sendEmailChangeNotification(User user, String oldEmail) {
        if (mailSender == null) {
            logger.error("JavaMailSender not configured for production!");
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(oldEmail);
            helper.setFrom(fromEmail, "DaScribs");
            helper.setSubject("Email Address Changed - DaScribs");

            String emailContent = buildEmailChangeNotificationEmail(user.getFullName(), user.getEmail());
            helper.setText(emailContent, true);

            mailSender.send(message);

            logger.info("✅ Email change notification sent to old email: {}", oldEmail);

        } catch (Exception e) {
            logger.error("❌ Failed to send email change notification to {}: {}", oldEmail, e.getMessage());
        }
    }

    @Override
    public void sendEmailChangeConfirmation(User user, String newEmail) {
        if (mailSender == null) {
            logger.error("JavaMailSender not configured for production!");
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(newEmail);
            helper.setFrom(fromEmail, "DaScribs");
            helper.setSubject("Email Change Confirmed - DaScribs");

            String emailContent = buildEmailChangeConfirmationEmail(user.getFullName(), newEmail);
            helper.setText(emailContent, true);

            mailSender.send(message);

            logger.info("✅ Email change confirmation sent to: {}", newEmail);

        } catch (Exception e) {
            logger.error("❌ Failed to send email change confirmation to {}: {}", newEmail, e.getMessage());
        }
    }

    // Email template methods
    private String buildVerificationEmail(String userName, String verificationLink) {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <style>
                body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 20px; }
                .container { max-width: 600px; margin: 0 auto; background: white; padding: 30px; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                .button { background-color: #28a745; color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; display: inline-block; font-weight: bold; }
                .footer { margin-top: 30px; padding-top: 20px; border-top: 1px solid #eee; color: #666; font-size: 14px; }
                .code { background: #f8f9fa; padding: 10px; border-radius: 4px; font-family: monospace; margin: 10px 0; }
            </style>
        </head>
        <body>
            <div class="container">
                <h2>✅ Verify Your Email Address</h2>
                <p>Hello <strong>%s</strong>,</p>
                <p>Welcome to DaScribs! Please verify your email address to activate your account and access all features.</p>
        
                <p style="text-align: center; margin: 30px 0;">
                    <a href="%s" class="button">Verify Email Address</a>
                </p>
        
                <p>Or copy and paste this link in your browser:</p>
                <div class="code">%s</div>
       
                <p><strong>⏰ This link will expire in 24 hours</strong> for security reasons.</p>
        
                <div class="footer">
                    <p>Best regards,<br><strong>DaScribs Team</strong></p>
                </div>
            </div>
        </body>
        </html>
        """.formatted(userName, verificationLink, verificationLink);
    }

    private String buildEmailChangeVerificationEmail(String userName, String newEmail, String verificationLink) {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <style>
                body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 20px; }
                .container { max-width: 600px; margin: 0 auto; background: white; padding: 30px; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                .button { background-color: #007bff; color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; display: inline-block; font-weight: bold; }
                .footer { margin-top: 30px; padding-top: 20px; border-top: 1px solid #eee; color: #666; font-size: 14px; }
                .alert { background-color: #fff3cd; padding: 15px; border-radius: 4px; border-left: 4px solid #ffc107; margin: 20px 0; }
            </style>
        </head>
        <body>
            <div class="container">
                <h2>📧 Confirm Your Email Change</h2>
                <p>Hello <strong>%s</strong>,</p>
                <p>You requested to change your email address to: <strong>%s</strong></p>
        
                <div class="alert">
                    <p><strong>Important:</strong> Please confirm this change by clicking the button below.</p>
                </div>
        
                <p style="text-align: center; margin: 30px 0;">
                    <a href="%s" class="button">Confirm Email Change</a>
                </p>
        
                <p><strong>⏰ This link will expire in 24 hours</strong> for security reasons.</p>
                <p>If you didn't request this change, please ignore this email and contact our support team immediately.</p>
        
                <div class="footer">
                    <p>Best regards,<br><strong>DaScribs Team</strong></p>
                </div>
            </div>
        </body>
        </html>
        """.formatted(userName, newEmail, verificationLink);
    }

    private String buildEmailChangeNotificationEmail(String userName, String newEmail) {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <style>
                body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 20px; }
                .container { max-width: 600px; margin: 0 auto; background: white; padding: 30px; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                .alert { background-color: #f8d7da; padding: 15px; border-radius: 4px; border-left: 4px solid #dc3545; margin: 20px 0; }
                .footer { margin-top: 30px; padding-top: 20px; border-top: 1px solid #eee; color: #666; font-size: 14px; }
            </style>
        </head>
        <body>
            <div class="container">
                <h2>⚠️ Email Address Changed</h2>
                <p>Hello <strong>%s</strong>,</p>
        
                <div class="alert">
                    <p><strong>Your DaScribs email address has been changed to: %s</strong></p>
                </div>
        
                <p><strong>If you made this change:</strong></p>
                <ul>
                    <li>No further action is required</li>
                    <li>You will now use your new email address to log in</li>
                </ul>
        
                <p><strong>If you didn't make this change:</strong></p>
                <ul>
                    <li>Contact our support team immediately</li>
                    <li>Secure your account by resetting your password</li>
                </ul>
        
                <div class="footer">
                    <p>Best regards,<br><strong>DaScribs Team</strong></p>
                </div>
            </div>
        </body>
        </html>
        """.formatted(userName, newEmail);
    }

    private String buildEmailChangeConfirmationEmail(String userName, String newEmail) {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <style>
                body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 20px; }
                .container { max-width: 600px; margin: 0 auto; background: white; padding: 30px; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                .success { background-color: #d4edda; padding: 15px; border-radius: 4px; border-left: 4px solid #28a745; margin: 20px 0; }
                .footer { margin-top: 30px; padding-top: 20px; border-top: 1px solid #eee; color: #666; font-size: 14px; }
            </style>
        </head>
        <body>
            <div class="container">
                <h2>✅ Email Change Confirmed</h2>
                <p>Hello <strong>%s</strong>,</p>
        
                <div class="success">
                    <p><strong>Your email address has been successfully changed to: %s</strong></p>
                </div>
        
                <p><strong>What's next?</strong></p>
                <ul>
                    <li>You will now use this email address to log in to DaScribs</li>
                    <li>All future communications will be sent to this address</li>
                    <li>Your account settings and data remain unchanged</li>
                </ul>
        
                <p>If you have any questions or need assistance, please contact our support team.</p>
        
                <div class="footer">
                    <p>Best regards,<br><strong>DaScribs Team</strong></p>
                </div>
            </div>
        </body>
        </html>
        """.formatted(userName, newEmail);
    }

    private String buildPasswordResetEmail(String userName, String resetLink) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 20px; }
                    .container { max-width: 600px; margin: 0 auto; background: white; padding: 30px; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                    .button { background-color: #007bff; color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; display: inline-block; font-weight: bold; }
                    .footer { margin-top: 30px; padding-top: 20px; border-top: 1px solid #eee; color: #666; font-size: 14px; }
                    .code { background: #f8f9fa; padding: 10px; border-radius: 4px; font-family: monospace; margin: 10px 0; }
                </style>
            </head>
            <body>
                <div class="container">
                    <h2>🔐 Password Reset Request</h2>
                    <p>Hello <strong>%s</strong>,</p>
                    <p>You requested to reset your password. Click the button below to create a new password:</p>
                    <p style="text-align: center; margin: 30px 0;">
                        <a href="%s" class="button">Reset Password</a>
                    </p>
                    <p>Or copy and paste this link in your browser:</p>
                    <div class="code">%s</div>
                    <p><strong>⏰ This link will expire in 1 hour</strong> for security reasons.</p>
                    <p>If you didn't request this, please ignore this email and your password will remain unchanged.</p>
                    <div class="footer">
                        <p>Best regards,<br><strong>DaScribs Team</strong></p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(userName, resetLink, resetLink);
    }

    private String buildPasswordChangedEmail(String userName) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 20px; }
                    .container { max-width: 600px; margin: 0 auto; background: white; padding: 30px; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                    .alert { background-color: #e8f5e8; padding: 15px; border-left: 4px solid #28a745; border-radius: 4px; }
                    .footer { margin-top: 30px; padding-top: 20px; border-top: 1px solid #eee; color: #666; font-size: 14px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <h2>✅ Password Changed Successfully</h2>
                    <p>Hello <strong>%s</strong>,</p>
            
                    <div class="alert">
                        <p><strong>Your DaScribs password was recently changed.</strong></p>
                    </div>
            
                    <p><strong>If you made this change:</strong></p>
                    <ul>
                        <li>No further action is required</li>
                        <li>All your existing sessions have been logged out for security</li>
                    </ul>
            
                    <p><strong>If you didn't make this change:</strong></p>
                    <ul>
                        <li>Immediately reset your password using the 'Forgot Password' feature</li>
                        <li>Contact our support team if you need assistance</li>
                    </ul>
            
                    <div class="footer">
                        <p>Best regards,<br><strong>DaScribs Team</strong></p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(userName);
    }

    private String buildWelcomeEmail(String userName, String userEmail, String temporaryPassword) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 20px; }
                    .container { max-width: 600px; margin: 0 auto; background: white; padding: 30px; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                    .credentials { background-color: #f8f9fa; padding: 20px; border-radius: 4px; border-left: 4px solid #007bff; margin: 20px 0; }
                    .footer { margin-top: 30px; padding-top: 20px; border-top: 1px solid #eee; color: #666; font-size: 14px; }
                    .warning { background-color: #fff3cd; padding: 15px; border-radius: 4px; border-left: 4px solid #ffc107; margin: 20px 0; }
                </style>
            </head>
            <body>
                <div class="container">
                    <h2>🎉 Welcome to DaScribs!</h2>
                    <p>Hello <strong>%s</strong>,</p>
                    <p>Your account has been successfully created. Welcome to DaScribs Real Estate CRM!</p>
           
                    <div class="credentials">
                        <h3>Your Login Credentials:</h3>
                        <p><strong>Email:</strong> %s</p>
                        <p><strong>Temporary Password:</strong> <code>%s</code></p>
                    </div>
            
                    <div class="warning">
                        <p><strong>🔒 Security Notice:</strong> For your security, please change your password immediately after first login.</p>
                    </div>
           
                    <p><strong>Next steps:</strong></p>
                    <ol>
                        <li>Log in to your account using the credentials above</li>
                        <li>Change your temporary password immediately</li>
                        <li>Complete your profile information</li>
                        <li>Explore the dashboard and features</li>
                    </ol>
            
                    <div class="footer">
                        <p>Best regards,<br><strong>DaScribs Team</strong></p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(userName, userEmail, temporaryPassword);
    }
}

// ==================== DEVELOPMENT ====================
@Component
@Profile("dev")
class DevelopmentEmailService implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(DevelopmentEmailService.class);

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Value("${app.email-service.dev-storage.file-path:./logs/emails}")
    private String fileStoragePath;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    @Override
    public void sendPasswordResetEmail(User user, String resetToken) {
        String resetLink = frontendUrl + "/reset-password?token=" + resetToken;

        logger.info("=== 🔐 PASSWORD RESET EMAIL (DEV MODE) ===");
        logger.info("To: {}", user.getEmail());
        logger.info("Subject: Password Reset Request - DaScribs");
        logger.info("User: {} ({})", user.getFullName(), user.getEmail());
        logger.info("Reset Link: {}", resetLink);
        logger.info("Token: {}", resetToken);
        logger.info("=== END EMAIL ===");

        saveEmailToFile("password-reset", user, resetToken, resetLink);
    }

    @Override
    public void sendPasswordChangedNotification(User user) {
        logger.info("=== ✅ PASSWORD CHANGE NOTIFICATION (DEV MODE) ===");
        logger.info("To: {}", user.getEmail());
        logger.info("Subject: Password Changed - DaScribs");
        logger.info("User: {} ({})", user.getFullName(), user.getEmail());
        logger.info("=== END EMAIL ===");

        saveEmailToFile("password-changed", user, null, null);
    }

    @Override
    public void sendWelcomeEmail(User user, String temporaryPassword) {
        logger.info("=== 🎉 WELCOME EMAIL (DEV MODE) ===");
        logger.info("To: {}", user.getEmail());
        logger.info("Subject: Welcome to DaScribs!");
        logger.info("User: {} ({})", user.getFullName(), user.getEmail());
        logger.info("Temporary Password: {}", temporaryPassword);
        logger.info("=== END EMAIL ===");

        saveEmailToFile("welcome", user, temporaryPassword, null);
    }

    @Override
    public void sendVerificationEmail(User user, String verificationToken) {
        String verificationLink = frontendUrl + "/verify-email?token=" + verificationToken;

        logger.info("=== ✅ EMAIL VERIFICATION (DEV MODE) ===");
        logger.info("To: {}", user.getEmail());
        logger.info("Subject: Verify Your Email - DaScribs");
        logger.info("User: {} ({})", user.getFullName(), user.getEmail());
        logger.info("Verification Link: {}", verificationLink);
        logger.info("Token: {}", verificationToken);
        logger.info("=== END EMAIL ===");

        saveEmailToFile("email-verification", user, verificationToken, verificationLink);
    }

    @Override
    public void sendEmailChangeVerification(User user, String newEmail, String verificationToken) {
        String verificationLink = frontendUrl + "/verify-email-change?token=" + verificationToken;

        logger.info("=== 📧 EMAIL CHANGE VERIFICATION (DEV MODE) ===");
        logger.info("To: {}", newEmail);
        logger.info("Subject: Confirm Your Email Change - DaScribs");
        logger.info("User: {} (old: {})", user.getFullName(), user.getEmail());
        logger.info("New Email: {}", newEmail);
        logger.info("Verification Link: {}", verificationLink);
        logger.info("Token: {}", verificationToken);
        logger.info("=== END EMAIL ===");

        saveEmailToFile("email-change-verification", user, verificationToken, verificationLink);
    }

    @Override
    public void sendEmailChangeNotification(User user, String oldEmail) {
        logger.info("=== ⚠️ EMAIL CHANGE NOTIFICATION (DEV MODE) ===");
        logger.info("To: {}", oldEmail);
        logger.info("Subject: Email Address Changed - DaScribs");
        logger.info("User: {} (old: {}, new: {})", user.getFullName(), oldEmail, user.getEmail());
        logger.info("=== END EMAIL ===");

        saveEmailToFile("email-change-notification", user, null, null);
    }

    @Override
    public void sendEmailChangeConfirmation(User user, String newEmail) {
        logger.info("=== ✅ EMAIL CHANGE CONFIRMATION (DEV MODE) ===");
        logger.info("To: {}", newEmail);
        logger.info("Subject: Email Change Confirmed - DaScribs");
        logger.info("User: {} ({})", user.getFullName(), newEmail);
        logger.info("=== END EMAIL ===");

        saveEmailToFile("email-change-confirmation", user, null, null);
    }

    private void saveEmailToFile(String emailType, User user, String tokenOrPassword, String link) {
        try {
            Path storageDir = Paths.get(fileStoragePath);
            if (!Files.exists(storageDir)) {
                Files.createDirectories(storageDir);
            }

            String timestamp = LocalDateTime.now().format(formatter);
            String filename = String.format("%s_%s_%s.txt",
                    emailType, user.getEmail().replace("@", "_"), timestamp);

            Path filePath = storageDir.resolve(filename);

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath.toFile()))) {
                writer.write("=== DA SCRIBS EMAIL (DEV MODE) ===\n");
                writer.write("Timestamp: " + LocalDateTime.now() + "\n");
                writer.write("Type: " + emailType + "\n");
                writer.write("To: " + user.getEmail() + "\n");
                writer.write("User: " + user.getFullName() + "\n");

                if (tokenOrPassword != null) {
                    if (emailType.equals("password-reset") || emailType.equals("email-verification") || emailType.equals("email-change-verification")) {
                        writer.write("Token: " + tokenOrPassword + "\n");
                        if (link != null) {
                            writer.write("Link: " + link + "\n");
                        }
                    } else if (emailType.equals("welcome")) {
                        writer.write("Temporary Password: " + tokenOrPassword + "\n");
                    }
                }

                writer.write("======================\n");
            }

            logger.debug("Email saved to: {}", filePath);

        } catch (IOException e) {
            logger.warn("Failed to save email to file: {}", e.getMessage());
        }
    }
}

// ==================== TEST ====================
@Component
@Profile("test")
class TestEmailService implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(TestEmailService.class);

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    // In-memory storage for test emails
    private final ConcurrentHashMap<String, TestEmail> emailStore = new ConcurrentHashMap<>();

    @Override
    public void sendPasswordResetEmail(User user, String resetToken) {
        String resetLink = frontendUrl + "/reset-password?token=" + resetToken;

        TestEmail email = new TestEmail(
                "password-reset",
                user,
                resetToken,
                resetLink,
                LocalDateTime.now()
        );

        String key = "reset_" + user.getEmail() + "_" + System.currentTimeMillis();
        emailStore.put(key, email);

        logger.info("=== 🔐 TEST PASSWORD RESET EMAIL ===");
        logger.info("To: {}", user.getEmail());
        logger.info("Reset Link: {}", resetLink);
        logger.info("Token: {}", resetToken);
        logger.info("Store Key: {}", key);
        logger.info("=== END TEST EMAIL ===");
    }

    @Override
    public void sendPasswordChangedNotification(User user) {
        TestEmail email = new TestEmail(
                "password-changed",
                user,
                null,
                null,
                LocalDateTime.now()
        );

        String key = "changed_" + user.getEmail() + "_" + System.currentTimeMillis();
        emailStore.put(key, email);

        logger.info("Password change notification stored for: {}", user.getEmail());
    }

    @Override
    public void sendWelcomeEmail(User user, String temporaryPassword) {
        TestEmail email = new TestEmail(
                "welcome",
                user,
                temporaryPassword,
                null,
                LocalDateTime.now()
        );

        String key = "welcome_" + user.getEmail() + "_" + System.currentTimeMillis();
        emailStore.put(key, email);

        logger.info("Welcome email stored for: {} with temp password: {}", user.getEmail(), temporaryPassword);
    }

    @Override
    public void sendVerificationEmail(User user, String verificationToken) {
        String verificationLink = frontendUrl + "/verify-email?token=" + verificationToken;

        TestEmail email = new TestEmail(
                "email-verification",
                user,
                verificationToken,
                verificationLink,
                LocalDateTime.now()
        );

        String key = "verify_" + user.getEmail() + "_" + System.currentTimeMillis();
        emailStore.put(key, email);

        logger.info("Verification email stored for: {}", user.getEmail());
    }

    @Override
    public void sendEmailChangeVerification(User user, String newEmail, String verificationToken) {
        String verificationLink = frontendUrl + "/verify-email-change?token=" + verificationToken;

        TestEmail email = new TestEmail(
                "email-change-verification",
                user,
                verificationToken,
                verificationLink,
                LocalDateTime.now()
        );

        String key = "change_verify_" + newEmail + "_" + System.currentTimeMillis();
        emailStore.put(key, email);

        logger.info("Email change verification stored for new email: {}", newEmail);
    }

    @Override
    public void sendEmailChangeNotification(User user, String oldEmail) {
        TestEmail email = new TestEmail(
                "email-change-notification",
                user,
                null,
                null,
                LocalDateTime.now()
        );

        String key = "change_notify_" + oldEmail + "_" + System.currentTimeMillis();
        emailStore.put(key, email);

        logger.info("Email change notification stored for old email: {}", oldEmail);
    }

    @Override
    public void sendEmailChangeConfirmation(User user, String newEmail) {
        TestEmail email = new TestEmail(
                "email-change-confirmation",
                user,
                null,
                null,
                LocalDateTime.now()
        );

        String key = "change_confirm_" + newEmail + "_" + System.currentTimeMillis();
        emailStore.put(key, email);

        logger.info("Email change confirmation stored for: {}", newEmail);
    }

    // Methods to retrieve test emails (useful for testing)
    public TestEmail getLatestEmailByType(String emailType) {
        return emailStore.entrySet().stream()
                .filter(entry -> entry.getValue().getType().equals(emailType))
                .reduce((first, second) -> second) // get last
                .map(Map.Entry::getValue)
                .orElse(null);
    }

    public TestEmail getLatestEmailByUser(String userEmail) {
        return emailStore.entrySet().stream()
                .filter(entry -> entry.getValue().getUser().getEmail().equals(userEmail))
                .reduce((first, second) -> second)
                .map(Map.Entry::getValue)
                .orElse(null);
    }

    public void clearEmails() {
        emailStore.clear();
    }

    // Test email data class
    public static class TestEmail {
        private final String type;
        private final User user;
        private final String tokenOrPassword;
        private final String link;
        private final LocalDateTime sentAt;

        public TestEmail(String type, User user, String tokenOrPassword, String link, LocalDateTime sentAt) {
            this.type = type;
            this.user = user;
            this.tokenOrPassword = tokenOrPassword;
            this.link = link;
            this.sentAt = sentAt;
        }

        // Getters
        public String getType() { return type; }
        public User getUser() { return user; }
        public String getTokenOrPassword() { return tokenOrPassword; }
        public String getLink() { return link; }
        public LocalDateTime getSentAt() { return sentAt; }
    }
}