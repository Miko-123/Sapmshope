package com.hopesapms.app.service;

import com.hopesapms.app.model.User;
import com.hopesapms.app.repository.UserRepository;
import com.hopesapms.app.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    public String generateTokenWithUserInfo(UserDetails userDetails) {
    User user = userRepository.findByUsernameAndIsDeletedFalse(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("User not found"));

    Map<String, Object> claims = new HashMap<>();
    claims.put("id", user.getId());
    claims.put("email", user.getEmail());

    return jwtUtil.generateToken(claims, userDetails);
}
}
