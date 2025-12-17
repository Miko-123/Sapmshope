package com.hopesapms.app.controller;

import com.hopesapms.app.service.AuthService;
import com.hopesapms.app.service.EmailService;
import com.hopesapms.app.service.OtpService;
import com.hopesapms.app.service.PasswordResetService;
import com.hopesapms.app.service.UserService;
import com.hopesapms.app.service.VerificationService;
import com.hopesapms.app.dto.MessageResponseDTO;
import com.hopesapms.app.dto.ResetPasswordRequest;
import com.hopesapms.app.dto.UserProfileUpdateRequest;
import com.hopesapms.app.dto.UserResponseDTO;
import com.hopesapms.app.dto.JwtResponse;
import com.hopesapms.app.model.User;
import com.hopesapms.app.dto.LoginRequest;
import com.hopesapms.app.dto.AuthProfileDTO;
import com.hopesapms.app.dto.ChangePasswordRequest;
import com.hopesapms.app.dto.CompleteProfileRequest;
import com.hopesapms.app.dto.ForgotPasswordRequest;
import com.hopesapms.app.repository.UserRepository;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final OtpService otpService;
    private final AuthService authService;
    private final VerificationService verificationService;
    private final EmailService emailService;
    private final UserService userService;

    @Autowired
    private PasswordResetService passwordResetService;

    @GetMapping("/me")
    public ResponseEntity<AuthProfileDTO> getCurrentUser(Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        User user = (User) authentication.getPrincipal();

        AuthProfileDTO userDto = new AuthProfileDTO(user);

        return ResponseEntity.ok(userDto);
    }

    @PutMapping("/profile")
    public ResponseEntity<UserResponseDTO> updateMyProfile(
            @Valid @RequestBody UserProfileUpdateRequest request,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        User currentUser = (User) authentication.getPrincipal();
        UserResponseDTO updatedProfile = userService.updateUserProfile(currentUser.getId(), request);

        return ResponseEntity.ok(updatedProfile);
    }

    @PostMapping("/change-password")
    public ResponseEntity<MessageResponseDTO> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        User currentUser = (User) authentication.getPrincipal();
        userService.changePassword(currentUser.getId(), request);

        return ResponseEntity.ok(new MessageResponseDTO("Password changed successfully"));
    }

    @PostMapping("/request-code")
    public ResponseEntity<MessageResponseDTO> sendCode(@RequestParam("email") String email) {
        User user = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (user.getPassword() != null) {
            return ResponseEntity.badRequest().body(new MessageResponseDTO("User account is already active"));
        }
        String otp = otpService.generateAndCacheOtp(email);
        emailService.sendVerificationEmail(email, otp);

        return ResponseEntity.ok(new MessageResponseDTO("Verification code sent to your email"));
    }

    @PostMapping("/verify-code")
    public ResponseEntity<MessageResponseDTO> verifyCode(@RequestParam("email") String email,
            @RequestParam("code") String code) {
        boolean success = otpService.validateOtp(email, code);
        if (success) {
            verificationService.cacheVerifiedEmail(email);
            return ResponseEntity
                    .ok(new MessageResponseDTO("Code verified successfully. Please complete your profile."));
        } else {
            return ResponseEntity.badRequest().body(new MessageResponseDTO("Invalid or expired code."));
        }
    }

    @PostMapping("/complete-profile")
    public ResponseEntity<JwtResponse> completeProfile(@Valid @RequestBody CompleteProfileRequest request) {
        JwtResponse jwtResponse = authService.completeProfile(request);
        return ResponseEntity.ok(jwtResponse);
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        passwordResetService.processForgotPassword(request.getEmail());
        return ResponseEntity.ok("If an account exists with that email, a reset link has been sent.");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok("Password has been successfully reset.");
    }
}