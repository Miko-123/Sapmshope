package com.hopesapms.app.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.hopesapms.app.dto.CompleteProfileRequest;
import com.hopesapms.app.dto.JwtResponse;
import com.hopesapms.app.dto.LoginRequest;
import com.hopesapms.app.model.User;
import com.hopesapms.app.repository.UserRepository;
import com.hopesapms.app.util.JwtUtil;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;

    private final VerificationService verificationService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    private final UserDetailsService userDetailsService;

    public JwtResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager
                .authenticate(
                        new UsernamePasswordAuthenticationToken(
                                request.getUsername(), request.getPassword()));
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String jwtToken = tokenService.generateTokenWithUserInfo(userDetails);
        return new JwtResponse(jwtToken);

    }

    @Transactional
    public JwtResponse completeProfile(CompleteProfileRequest request) {

        if (!verificationService.validateVerifiedEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email verification is invalid or has expired. Please try again.");
        }

        User user = userRepository.findByEmailAndIsDeletedFalse(request.getEmail())
                .orElseThrow(() -> new EntityNotFoundException("User not found."));

        if (user.getPassword() != null) {
            throw new IllegalArgumentException("User profile is already complete.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setGender(request.getGender());
        user.setPhoneNumber(request.getPhoneNumber());
        

        User savedUser = userRepository.save(user);

        auditLogService.log("COMPLETE_PROFILE", "User", savedUser.getId().longValue(), null, "User profile completed and password set.");

       
        UserDetails userDetails = userDetailsService.loadUserByUsername(savedUser.getUsername());
        String jwtToken = tokenService.generateTokenWithUserInfo(userDetails);
        
        return new JwtResponse(jwtToken);
    }
}
