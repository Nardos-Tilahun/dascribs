package com.dascribs.coreauth.repository;

import com.dascribs.coreauth.entity.auth.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);

    Optional<PasswordResetToken> findByUserIdAndUsedFalse(Long userId);

    @Query("SELECT prt FROM PasswordResetToken prt WHERE prt.user.email = :email AND prt.used = false AND prt.expiresAt > :now")
    Optional<PasswordResetToken> findValidTokenByUserEmail(@Param("email") String email, @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE PasswordResetToken prt SET prt.used = true WHERE prt.user.id = :userId AND prt.used = false")
    void invalidateAllUserTokens(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM PasswordResetToken prt WHERE prt.expiresAt < :now")
    void deleteExpiredTokens(@Param("now") LocalDateTime now);

    @Query("SELECT COUNT(prt) FROM PasswordResetToken prt WHERE prt.user.id = :userId AND prt.createdAt > :since")
    long countRecentTokensByUserId(@Param("userId") Long userId, @Param("since") LocalDateTime since);
}