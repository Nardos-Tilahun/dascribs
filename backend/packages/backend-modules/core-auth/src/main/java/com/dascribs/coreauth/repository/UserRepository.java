package com.dascribs.coreauth.repository;

import com.dascribs.coreauth.entity.user.Role;
import com.dascribs.coreauth.entity.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Find user by email
    Optional<User> findByEmail(String email);

    // Check if email exists
    boolean existsByEmail(String email);

    // Find users by role
    List<User> findByRole(Role role);

    // Find active users
    List<User> findByActiveTrue();

    // Find active users with pagination
    Page<User> findByActiveTrue(Pageable pageable);

    // Find users by multiple roles
    @Query("SELECT u FROM User u WHERE u.role IN :roles")
    List<User> findByRoles(@Param("roles") List<Role> roles);

    // Count active users
    @Query("SELECT COUNT(u) FROM User u WHERE u.active = true")
    long countActiveUsers();

    // Search users by email pattern
    @Query("SELECT u FROM User u WHERE u.email LIKE %:email%")
    List<User> findByEmailContaining(@Param("email") String email);

    // Find active users by role
    @Query("SELECT u FROM User u WHERE u.role = :role AND u.active = true")
    List<User> findActiveByRole(@Param("role") Role role);

    // Find users by tenant ID (multi-tenancy)
    @Query("SELECT u FROM User u JOIN u.userTenants ut WHERE ut.tenant.id = :tenantId")
    Page<User> findByTenantId(@Param("tenantId") Long tenantId, Pageable pageable);

    // Find users by tenant ID and role
    @Query("SELECT u FROM User u JOIN u.userTenants ut WHERE ut.tenant.id = :tenantId AND u.role = :role")
    List<User> findByTenantIdAndRole(@Param("tenantId") Long tenantId, @Param("role") Role role);

    // Find users by tenant ID with active status
    @Query("SELECT u FROM User u JOIN u.userTenants ut WHERE ut.tenant.id = :tenantId AND u.active = :active")
    Page<User> findByTenantIdAndActive(@Param("tenantId") Long tenantId, @Param("active") boolean active, Pageable pageable);

    // Search users by name within a tenant
    @Query("SELECT u FROM User u JOIN u.userTenants ut WHERE ut.tenant.id = :tenantId AND u.fullName LIKE %:name%")
    Page<User> findByTenantIdAndNameContaining(@Param("tenantId") Long tenantId, @Param("name") String name, Pageable pageable);
}