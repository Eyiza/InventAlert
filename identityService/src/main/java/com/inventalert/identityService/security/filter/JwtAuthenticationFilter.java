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
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);
        if (!jwtUtil.isTokenValid(token)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
            return;
        }

        String role      = jwtUtil.extractRole(token);
        String companyId = jwtUtil.extractCompanyId(token);
        String userId    = jwtUtil.extractUserId(token);

        if (!"SUPER_ADMIN".equals(role)) {
            if (companyId != null) {
                boolean suspended = companyRepository.findById(companyId)
                        .map(c -> c.getStatus() == CompanyStatus.SUSPENDED)
                        .orElse(false);
                if (suspended) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Company account is suspended");
                    return;
                }
            }

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
