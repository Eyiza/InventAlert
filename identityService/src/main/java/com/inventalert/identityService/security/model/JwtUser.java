package com.inventalert.identityService.security.model;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Immutable principal injected via @AuthenticationPrincipal in every controller.
 * Full class path: com.inventalert.identityService.security.model.JwtUser
 * Dev A: import this and use @AuthenticationPrincipal JwtUser principal in controllers.
 */
public record JwtUser(
        String userId,
        String companyId,    // null for SuperAdmin tokens
        String role,
        String warehouseId   // null unless role is WAREHOUSE_STAFF
) implements UserDetails {

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override public String getPassword()               { return null; }
    @Override public String getUsername()               { return userId; }
    @Override public boolean isAccountNonExpired()      { return true; }
    @Override public boolean isAccountNonLocked()       { return true; }
    @Override public boolean isCredentialsNonExpired()  { return true; }
    @Override public boolean isEnabled()                { return true; }
}
