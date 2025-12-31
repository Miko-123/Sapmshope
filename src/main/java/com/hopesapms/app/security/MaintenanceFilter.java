package com.hopesapms.app.security;

import com.hopesapms.app.service.SystemSettingsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class MaintenanceFilter extends OncePerRequestFilter {

    private final SystemSettingsService systemSettingsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        if (path.startsWith("/api/auth") || path.startsWith("/swagger") || path.startsWith("/v3/api-docs")) {
            filterChain.doFilter(request, response);
            return;
        }

        if (systemSettingsService.isMaintenanceMode()) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            boolean isAdmin = auth != null && auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("SYSTEM_ADMIN"));

            if (!isAdmin) {
                response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                response.setContentType("application/json");
                response.getWriter()
                        .write("{\"message\": \"System is currently under maintenance. Please try again later.\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
