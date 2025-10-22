package com.dascribs.coreauth.repository;

import com.dascribs.coreauth.entity.tenant.Tenant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, Long> {

    // Find tenant by unique tenant ID
    Optional<Tenant> findByTenantId(String tenantId);

    // Find tenant by domain
    Optional<Tenant> findByDomain(String domain);

    // Check if tenant ID exists
    boolean existsByTenantId(String tenantId);

    // Check if domain exists
    boolean existsByDomain(String domain);

    // Find active tenants with pagination
    Page<Tenant> findByActiveTrue(Pageable pageable);

    // Count active tenants
    @Query("SELECT COUNT(t) FROM Tenant t WHERE t.active = true")
    long countActiveTenants();

    // Search tenants by name pattern
    @Query("SELECT t FROM Tenant t WHERE t.name LIKE %:name%")
    Page<Tenant> findByNameContaining(@Param("name") String name, Pageable pageable);

    // Find tenants by plan type
    Page<Tenant> findByPlan(Tenant.Plan plan, Pageable pageable);

    // Find active tenants by plan
    @Query("SELECT t FROM Tenant t WHERE t.plan = :plan AND t.active = true")
    Page<Tenant> findByPlanAndActiveTrue(@Param("plan") Tenant.Plan plan, Pageable pageable);

    // Count tenants by plan
    @Query("SELECT COUNT(t) FROM Tenant t WHERE t.plan = :plan")
    long countByPlan(@Param("plan") Tenant.Plan plan);

    // Find tenants with subscription ending soon
    @Query("SELECT t FROM Tenant t WHERE t.subscriptionEndsAt IS NOT NULL AND t.subscriptionEndsAt <= FUNCTION('timestamp_add_days', CURRENT_TIMESTAMP(), 30)")
    List<Tenant> findTenantsWithSubscriptionEndingSoon();

    // Check if tenant has reached user limit
    @Query("SELECT CASE WHEN COUNT(ut) >= t.maxUsers THEN true ELSE false END " +
            "FROM Tenant t LEFT JOIN t.userTenants ut " +
            "WHERE t.id = :tenantId AND ut.user.active = true")
    boolean hasReachedUserLimit(@Param("tenantId") Long tenantId);

    // Check if tenant has reached property limit
//    @Query("SELECT CASE WHEN (SELECT COUNT(p) FROM Property p WHERE p.tenant.id = :tenantId) >= t.maxProperties THEN true ELSE false END " +
//            "FROM Tenant t WHERE t.id = :tenantId")
//    boolean hasReachedPropertyLimit(@Param("tenantId") Long tenantId);
}