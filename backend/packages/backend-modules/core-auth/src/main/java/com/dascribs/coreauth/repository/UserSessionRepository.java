package com.dascribs.coreauth.repository;

import com.dascribs.coreauth.entity.user.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, Long> {

    // Find session by token
    Optional<UserSession> findBySessionToken(String sessionToken);

    // Find all active sessions for a user
    List<UserSession> findByUserIdAndExpiresAtAfter(Long userId, LocalDateTime now);

    // Find all sessions for a user
    List<UserSession> findByUserId(Long userId);

    // Check if session exists and is valid
    @Query("SELECT COUNT(us) > 0 FROM UserSession us WHERE us.sessionToken = :token AND us.expiresAt > :now")
    boolean existsValidSessionByToken(@Param("token") String token, @Param("now") LocalDateTime now);

    // Delete expired sessions
    @Modifying
    @Query("DELETE FROM UserSession us WHERE us.expiresAt < :now")
    void deleteExpiredSessions(@Param("now") LocalDateTime now);

    // Delete all sessions for a user
    @Modifying
    @Query("DELETE FROM UserSession us WHERE us.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);

    // Delete specific session by token
    @Modifying
    @Query("DELETE FROM UserSession us WHERE us.sessionToken = :token")
    void deleteBySessionToken(@Param("token") String token);

    // Update last activity for a session
    @Modifying
    @Query("UPDATE UserSession us SET us.lastActivityAt = :lastActivityAt WHERE us.sessionToken = :token")
    void updateLastActivity(@Param("token") String token, @Param("lastActivityAt") LocalDateTime lastActivityAt);

    // Count active sessions for a user
    @Query("SELECT COUNT(us) FROM UserSession us WHERE us.user.id = :userId AND us.expiresAt > :now")
    long countActiveSessionsByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    // Find sessions by IP address
    List<UserSession> findByIpAddress(String ipAddress);

    // Find sessions that need to be expired soon (for notifications)
    @Query("SELECT us FROM UserSession us WHERE us.expiresAt BETWEEN :start AND :end")
    List<UserSession> findSessionsExpiringBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}