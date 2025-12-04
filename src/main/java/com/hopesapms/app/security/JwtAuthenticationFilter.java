package com.hopesapms.app.security;

import com.hopesapms.app.service.CustomUserDetailsService;
import com.hopesapms.app.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;

    private static final List<String> EXCLUDED_PATTERNS = List.of(
           "/v3/api-docs/**",
        "/swagger-ui/**",
        "/swagger-ui.html",
        
        "/api/auth/login",
        "/api/auth/register",
        "/api/auth/verify-email",
        "/api/auth/complete-profile",
        "/api/auth/request-code",  
        "/api/auth/verify-code",    

        "/error",
        "/favicon.ico"
    );

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        return EXCLUDED_PATTERNS.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        System.out.println("--- JwtAuthenticationFilter triggered for: " + request.getRequestURI());

        final String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        System.out.println("Raw Authorization header: [" + authHeader + "]");
        final String token = authHeader.replaceFirst("(?i)^Bearer\\s+", "").trim();

        System.out.println(">>>TOKEN START<<<" + token + ">>>TOKEN END<<<");
        System.out.println("Token length: " + token.length());

        final String username;
        try {
            username = jwtUtil.extractUsername(token);
        } catch (Exception e) {
            System.out.println("--- Invalid JWT format: " + e.getMessage());
            filterChain.doFilter(request, response);
            return;
        }

        System.out.println("--- Username extracted from token: " + username);

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            if (jwtUtil.isTokenValid(token, userDetails)) {
                System.out.println("--- Token is VALID. Setting authentication in context.");
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            } else {
                System.out.println("--- Token is INVALID.");
            }
        }
        filterChain.doFilter(request, response);
    }
}