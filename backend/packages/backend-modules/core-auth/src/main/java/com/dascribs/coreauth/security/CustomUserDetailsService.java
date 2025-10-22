package com.dascribs.coreauth.security;


import com.dascribs.coreauth.entity.user.User;
import com.dascribs.coreauth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        if (!user.isActive()) {
            throw new UsernameNotFoundException("User account is deactivated");
        }

        if (!user.isEmailVerified()) {
            throw new UsernameNotFoundException("Email not verified");
        }

        return UserPrincipal.create(user);
    }

    @Transactional
    public UserDetails loadUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + id));

        if (!user.isActive()) {
            throw new UsernameNotFoundException("User account is deactivated");
        }

        return UserPrincipal.create(user);
    }

    public static class UserPrincipal implements UserDetails {
        private Long id;
        private String email;
        private String password;
        private Collection<? extends GrantedAuthority> authorities;
        private boolean active;

        public UserPrincipal(Long id, String email, String password,
                             Collection<? extends GrantedAuthority> authorities, boolean active) {
            this.id = id;
            this.email = email;
            this.password = password;
            this.authorities = authorities;
            this.active = active;
        }

        public static UserPrincipal create(User user) {
            // Convert permissions to authorities
            List<GrantedAuthority> authorities = user.getRole().getPermissions().stream()
                    .map(permission -> new SimpleGrantedAuthority(permission.name()))
                    .collect(Collectors.toList());

            // Add ROLE_ prefix for Spring Security
            authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));

            return new UserPrincipal(
                    user.getId(),
                    user.getEmail(),
                    user.getPassword(),
                    authorities,
                    user.isActive()
            );
        }

        // UserDetails methods
        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return authorities;
        }

        @Override
        public String getPassword() {
            return password;
        }

        @Override
        public String getUsername() {
            return email;
        }

        @Override
        public boolean isAccountNonExpired() {
            return true;
        }

        @Override
        public boolean isAccountNonLocked() {
            return true;
        }

        @Override
        public boolean isCredentialsNonExpired() {
            return true;
        }

        @Override
        public boolean isEnabled() {
            return active;
        }

        // Custom methods
        public Long getId() {
            return id;
        }

        public String getEmail() {
            return email;
        }

        public boolean hasPermission(String permission) {
            return authorities.stream()
                    .anyMatch(auth -> auth.getAuthority().equals(permission));
        }

        public boolean hasRole(String role) {
            return authorities.stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_" + role));
        }
    }
}