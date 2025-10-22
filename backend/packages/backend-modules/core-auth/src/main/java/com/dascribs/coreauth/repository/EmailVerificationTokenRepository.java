package com.dascribs.coreauth.repository;

import com.dascribs.coreauth.entity.auth.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findByToken(String token);

    List<EmailVerificationToken> findByUserIdAndTokenTypeAndUsedFalse(Long userId, EmailVerificationToken.TokenType tokenType);

    Optional<EmailVerificationToken> findByUserIdAndTokenTypeAndEmailAndUsedFalse(
            Long userId, EmailVerificationToken.TokenType tokenType, String email);

    @Modifying
    @Query("UPDATE EmailVerificationToken evt SET evt.used = true WHERE evt.user.id = :userId AND evt.tokenType = :tokenType AND evt.used = false")
    void invalidateUserTokens(@Param("userId") Long userId, @Param("tokenType") EmailVerificationToken.TokenType tokenType);

    @Modifying
    @Query("DELETE FROM EmailVerificationToken evt WHERE evt.expiresAt < :now")
    void deleteExpiredTokens(@Param("now") LocalDateTime now);

    @Query("SELECT COUNT(evt) FROM EmailVerificationToken evt WHERE evt.user.id = :userId AND evt.createdAt > :since")
    long countRecentTokensByUserId(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    @Query("SELECT evt FROM EmailVerificationToken evt WHERE evt.user.id = :userId AND evt.tokenType = :tokenType AND evt.used = false ORDER BY evt.createdAt DESC")
    List<EmailVerificationToken> findActiveTokensByUserAndType(@Param("userId") Long userId, @Param("tokenType") EmailVerificationToken.TokenType tokenType);
}