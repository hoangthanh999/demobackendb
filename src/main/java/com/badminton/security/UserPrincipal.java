// security/UserPrincipal.java
package com.badminton.security;

import com.badminton.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * UserPrincipal - Đại diện user trong Security Context
 * Tương thích với JWT dùng email làm subject
 */
@Data
@AllArgsConstructor
public class UserPrincipal implements UserDetails {

    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private String password;
    private Collection<? extends GrantedAuthority> authorities;
    private Boolean active;

    /**
     * Tạo UserPrincipal từ User entity
     */
    public static UserPrincipal create(User user) {
        Collection<GrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));

        return new UserPrincipal(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getPassword(),
                authorities,
                user.getActive());
    }

    // ========== UserDetails Interface Methods ==========

    @Override
    public String getUsername() {
        return email; // ✅ Trả về email (tương thích với JWT)
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
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
        return active != null && active;
    }
}
