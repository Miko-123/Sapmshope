package com.hopesapms.app.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JwtAuthEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException, ServletException {

        // Use the "jakarta" attributes, not "javax"
        request.setAttribute("jakarta.servlet.error.status_code", HttpServletResponse.SC_UNAUTHORIZED);
        request.setAttribute("jakarta.servlet.error.message", authException.getMessage());

        // You can also set the response status directly, which is cleaner
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        request.getRequestDispatcher("/error").forward(request, response);
    }
}