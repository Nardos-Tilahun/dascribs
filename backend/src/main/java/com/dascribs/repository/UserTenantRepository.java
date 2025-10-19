package com.dascribs.repository;

import com.dascribs.model.user.UserTenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserTenantRepository extends JpaRepository<UserTenant, Long> {

    // Find user-tenant relationship by user ID and tenant ID
    Optional<UserTenant> findByUserIdAndTenantId(Long userId, Long tenantId);

    // Find all user-tenant relationships for a user
    List<UserTenant> findByUserId(Long userId);

    // Find all user-tenant relationships for a tenant
    List<UserTenant> findByTenantId(Long tenantId);

    // Find primary tenant for a user
    @Query("SELECT ut FROM UserTenant ut WHERE ut.user.id = :userId AND ut.isPrimary = true")
    Optional<UserTenant> findPrimaryTenantByUserId(@Param("userId") Long userId);

    // Count users in a tenant
    @Query("SELECT COUNT(ut) FROM UserTenant ut WHERE ut.tenant.id = :tenantId")
    long countUsersByTenantId(@Param("tenantId") Long tenantId);

    // Count active users in a tenant
    @Query("SELECT COUNT(ut) FROM UserTenant ut WHERE ut.tenant.id = :tenantId AND ut.user.active = true")
    long countActiveUsersByTenantId(@Param("tenantId") Long tenantId);

    // Check if user-tenant relationship exists
    boolean existsByUserIdAndTenantId(Long userId, Long tenantId);

    // Find all users for a tenant with pagination
    @Query("SELECT ut FROM UserTenant ut WHERE ut.tenant.id = :tenantId")
    List<UserTenant> findByTenantIdWithPagination(@Param("tenantId") Long tenantId, org.springframework.data.domain.Pageable pageable);

    // Find user's tenants with user details
    @Query("SELECT ut FROM UserTenant ut JOIN FETCH ut.tenant WHERE ut.user.id = :userId")
    List<UserTenant> findByUserIdWithTenantDetails(@Param("userId") Long userId);

    // Find tenant's users with user details
    @Query("SELECT ut FROM UserTenant ut JOIN FETCH ut.user WHERE ut.tenant.id = :tenantId")
    List<UserTenant> findByTenantIdWithUserDetails(@Param("tenantId") Long tenantId);

    // Find all primary user-tenant relationships
    @Query("SELECT ut FROM UserTenant ut WHERE ut.isPrimary = true")
    List<UserTenant> findAllPrimaryRelationships();

    // Update primary tenant for a user
    @Query("UPDATE UserTenant ut SET ut.isPrimary = false WHERE ut.user.id = :userId")
    void clearPrimaryTenantForUser(@Param("userId") Long userId);

    // Check if user has any tenant association
    @Query("SELECT COUNT(ut) > 0 FROM UserTenant ut WHERE ut.user.id = :userId")
    boolean userHasTenantAssociation(@Param("userId") Long userId);

    // Find user's tenants by user email
    @Query("SELECT ut FROM UserTenant ut JOIN ut.user u WHERE u.email = :email")
    List<UserTenant> findByUserEmail(@Param("email") String email);
}