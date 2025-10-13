package com.hopesapms.app.service;

import com.hopesapms.app.model.User;
import com.hopesapms.app.model.VerificationCode;
import com.hopesapms.app.repository.UserRepository;
import com.hopesapms.app.repository.VerificationCodeRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class VerificationService {
    private final VerificationCodeRepository codeRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    private final Random random = new Random();

    @Transactional
    public void sendCode(String email) {
        User user = userRepository.findByEmailAndIsDeletedFalse(email.trim()).orElseThrow(() -> new IllegalArgumentException("No user found with email " + email));

        String code = String.format("%06d", random.nextInt(999999));

        VerificationCode verificationCode = VerificationCode.builder()
        .user(user)
        .code(code)
        .expiryTime(LocalDateTime.now().plusMinutes(10))
        .used(false)
        .build();

        codeRepository.save(verificationCode);

        emailService.sendVerificationEmail(user.getEmail(), code);
    }

    @Transactional
    public boolean verifycode(String email, String code){
        User user = userRepository.findByEmailAndIsDeletedFalse(email)
        .orElseThrow(() -> new IllegalArgumentException("No user found with email " + email));
        
        VerificationCode verificationCode = codeRepository.findByUserAndCodeAndUsedFalse(user, code)
        .orElseThrow(() -> new IllegalArgumentException("Invalid or already used code"));

        if (verificationCode.getExpiryTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Code exprired");
        }

        verificationCode.setUsed(true);
        codeRepository.save(verificationCode);
        
        return true;
    }
}
