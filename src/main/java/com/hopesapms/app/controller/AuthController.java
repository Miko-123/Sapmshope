package com.hopesapms.app.controller;

import com.hopesapms.app.service.AuthService;
import com.hopesapms.app.service.EmailService;
import com.hopesapms.app.service.OtpService;
import com.hopesapms.app.service.VerificationService;
import com.hopesapms.app.dto.*;
import com.hopesapms.app.model.User;
import com.hopesapms.app.repository.UserRepository;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;



import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final OtpService otpService;
    private final AuthService authService;
    private final VerificationService verificationService;
    private final EmailService emailService;

    @PostMapping("/request-code")
    public ResponseEntity<String> sendCode(@RequestParam("email") String email) {
        User user = userRepository.findByEmailAndIsDeletedFalse(email).orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (user.getPassword() != null) {
            return ResponseEntity.badRequest().body("User account is already active");
        }
        String otp = otpService.generateAndCacheOtp(email);
        emailService.sendVerificationEmail(email, otp);
        
        return ResponseEntity.ok("Verification code sent to your email");
    }

    @PostMapping("/verify-code")
    public ResponseEntity<String> verifyCode(@RequestParam("email") String eamil, @RequestParam("code") String code) {
        boolean success = otpService.validateOtp(eamil, code);
        if (success){
            verificationService.cacheVerifiedEmail(eamil);
            return ResponseEntity.ok("Code verified successfully. Please complete your profile.");
        }
        else {
            return ResponseEntity.badRequest().body("Invalid or expired code.");
        }
    }

    @PostMapping("/complete-profile")
    public ResponseEntity <JwtResponse> completeProfile(@Valid @RequestBody CompleteProfileRequest request) {
        JwtResponse jwtResponse = authService.completeProfile(request);
        return ResponseEntity.ok(jwtResponse);
    }
    
     
    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
