package com.hopesapms.app.modules.auth.service;

import com.hopesapms.app.modules.user.model.User;
import com.hopesapms.app.modules.user.repository.UserRepository;
import com.hopesapms.app.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.security.core.userdetails.UserDetails;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    public String generateTokenWithUserInfo(UserDetails userDetails) {
        User user = userRepository.findByEmailAndIsDeletedFalse(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Map<String, Object> claims = new HashMap<>();
        claims.put("id", user.getId());
        claims.put("email", user.getEmail());

        return jwtUtil.generateToken(claims, userDetails);
    }
}