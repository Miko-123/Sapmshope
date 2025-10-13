package com.hopesapms.app.controller;

import com.hopesapms.app.service.VerificationService;
import com.hopesapms.app.util.JwtUtil;
import com.hopesapms.app.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final VerificationService verificationService;

    @PostMapping("/send-code")
    public ResponseEntity<String> sendCode(@RequestParam("email") String email) {
        verificationService.sendCode(email);
        return ResponseEntity.ok("Verification code send to email");
    }

    @PostMapping("/verify-code")
    public ResponseEntity<String> verifyCode(@RequestParam("email") String eamil, @RequestParam("code") String code) {
        boolean success = verificationService.verifycode(eamil, code);
        return success ? ResponseEntity.ok("Code verified successfully") : ResponseEntity.badRequest().body("Invalid code");
    }
     
    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwtToken = jwtUtil.generateToken((UserDetails) authentication.getPrincipal());

        return ResponseEntity.ok(new JwtResponse(jwtToken));
    }
}
