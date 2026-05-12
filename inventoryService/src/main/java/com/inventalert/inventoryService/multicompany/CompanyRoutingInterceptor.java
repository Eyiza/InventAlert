package com.inventalert.inventoryService.multicompany;

import com.inventalert.inventoryService.security.service.JwtUtil;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class CompanyRoutingInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                String companyId = jwtUtil.extractCompanyId(token);
                if (companyId != null) {
                    CompanyContext.set(companyId);
                }
            } catch (JwtException ignored) {
                // Security filter handles invalid tokens; interceptor only routes valid ones
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                 Object handler, Exception ex) {
        CompanyContext.clear();
    }
}
