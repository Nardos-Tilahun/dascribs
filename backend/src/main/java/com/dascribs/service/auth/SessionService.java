package com.dascribs.service.auth;

import com.dascribs.model.user.User;
import com.dascribs.model.user.UserSession;
import com.dascribs.repository.UserSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class SessionService {

    @Autowired
    private UserSessionRepository userSessionRepository;

    @Value("${app.security.session.timeout-minutes:120}")
    private int sessionTimeoutMinutes;

    @Value("${app.security.session.max-sessions-per-user:5}")
    private int maxSessionsPerUser;

    public UserSession createSession(User user, String ipAddress, String userAgent) {
        // Clean up old sessions if user has too many
        enforceSessionLimit(user.getId());

        String sessionToken = generateSessionToken();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(sessionTimeoutMinutes);

        UserSession session = new UserSession(user, sessionToken, ipAddress, userAgent, expiresAt);
        return userSessionRepository.save(session);
    }

    public Optional<UserSession> validateSession(String sessionToken) {
        Optional<UserSession> sessionOpt = userSessionRepository.findBySessionToken(sessionToken);

        if (sessionOpt.isPresent()) {
            UserSession session = sessionOpt.get();

            if (session.isValid()) {
                // Update last activity
                session.updateLastActivity();
                userSessionRepository.save(session);
                return Optional.of(session);
            } else {
                // Session expired, delete it
                userSessionRepository.delete(session);
            }
        }

        return Optional.empty();
    }

    public void logoutSession(String sessionToken) {
        userSessionRepository.deleteBySessionToken(sessionToken);
    }

    public void logoutAllUserSessions(Long userId) {
        userSessionRepository.deleteAllByUserId(userId);
    }

    public List<UserSession> getUserSessions(Long userId) {
        return userSessionRepository.findByUserId(userId);
    }

    public List<UserSession> getActiveUserSessions(Long userId) {
        return userSessionRepository.findByUserIdAndExpiresAtAfter(userId, LocalDateTime.now());
    }

    public void cleanupExpiredSessions() {
        userSessionRepository.deleteExpiredSessions(LocalDateTime.now());
    }

    public void updateLastActivity(String sessionToken) {
        userSessionRepository.updateLastActivity(sessionToken, LocalDateTime.now());
    }

    private String generateSessionToken() {
        return UUID.randomUUID().toString() + "-" + System.currentTimeMillis();
    }

    private void enforceSessionLimit(Long userId) {
        List<UserSession> activeSessions = getActiveUserSessions(userId);

        if (activeSessions.size() >= maxSessionsPerUser) {
            // Remove oldest session
            UserSession oldestSession = activeSessions.stream()
                    .min((s1, s2) -> s1.getLastActivityAt().compareTo(s2.getLastActivityAt()))
                    .orElse(null);

            if (oldestSession != null) {
                userSessionRepository.delete(oldestSession);
            }
        }
    }

    public int getActiveSessionCount(Long userId) {
        return userSessionRepository.findByUserIdAndExpiresAtAfter(userId, LocalDateTime.now()).size();
    }

    public boolean isSessionLimitReached(Long userId) {
        return getActiveSessionCount(userId) >= maxSessionsPerUser;
    }
}