package com.donation.security;

import com.donation.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Set;

@Getter
public class CustomUserPrincipal implements UserDetails {

    private final User user;
    private final Set<? extends GrantedAuthority> authorities;

    public CustomUserPrincipal(User user, Set<? extends GrantedAuthority> authorities) {
        this.user = user;
        this.authorities = authorities;
    }

    public Long getUserId() {
        return user.getId();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return user.isActive();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.isActive();
    }

    public boolean hasPermission(String permission) {
        return authorities.stream()
            .anyMatch(a -> a.getAuthority().equals(permission));
    }

    public boolean isSuperAdmin() {
        return authorities.stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"));
    }
}
