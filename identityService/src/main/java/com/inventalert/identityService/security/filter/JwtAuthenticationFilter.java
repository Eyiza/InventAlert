package com.inventalert.identityService.security.filter;

import com.inventalert.identityService.model.CompanyStatus;
import com.inventalert.identityService.repository.CompanyRepository;
import com.inventalert.identityService.repository.UserRepository;
import com.inventalert.identityService.security.model.JwtUser;
import com.inventalert.identityService.security.service.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        // Pass unauthenticated requests through — Spring Security denies them at the method level
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);
        // Invalid/expired tokens are not rejected here; they simply produce no authentication,
        // letting Spring Security return 401 for protected endpoints as normal
        if (!jwtUtil.isTokenValid(token)) {
            chain.doFilter(request, response);
            return;
        }

        String role      = jwtUtil.extractRole(token);
        String companyId = jwtUtil.extractCompanyId(token);
        String userId    = jwtUtil.extractUserId(token);

        // SUPER_ADMIN tokens have no companyId and are never tenant-scoped, so the
        // company-suspension and user-deactivation checks are intentionally skipped
        if (!"SUPER_ADMIN".equals(role)) {
            if (companyId != null) {
                boolean suspended = companyRepository.findById(companyId)
                        .map(c -> c.getStatus() == CompanyStatus.SUSPENDED)
                        .orElse(false);
                // Reject mid-session if the company was suspended after the token was issued
                if (suspended) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Company account is suspended");
                    return;
                }
            }

            // orElse(true) treats a deleted user as deactivated, preventing stale tokens from working
            boolean deactivated = userRepository.findById(userId)
                    .map(u -> !u.isActive())
                    .orElse(true);
            if (deactivated) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "User account is deactivated");
                return;
            }
        }

        JwtUser principal = new JwtUser(
                userId,
                companyId,
                role,
                jwtUtil.extractWarehouseId(token)
        );

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        chain.doFilter(request, response);
    }
}
