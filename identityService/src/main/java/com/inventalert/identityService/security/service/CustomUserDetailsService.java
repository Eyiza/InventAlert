package com.inventalert.identityService.security.service;

import com.inventalert.identityService.repository.UserRepository;
import com.inventalert.identityService.security.model.JwtUser;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Used internally by Spring Security's DaoAuthenticationProvider during password verification.
 * The "username" here is actually the userId (JWT subject).
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userId));
        return new JwtUser(
                user.getId(),
                user.getCompanyId(),
                user.getRole().name(),
                user.getWarehouseId()
        );
    }
}
