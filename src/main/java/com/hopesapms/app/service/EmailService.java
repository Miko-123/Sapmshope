package com.hopesapms.app.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.hopesapms.app.model.User;
import com.hopesapms.app.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailService {
    
    private final JavaMailSender mailSender;
    private final AuditLogService auditLogService;
    private final UserRepository userRepository;


    public void sendVerificationEmail(String to, String code) {
    try {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Your Verification Code");
        message.setText("Hello,\n\nYour verification code is: " + code + "\n\nRegards,\nHOPE SAPMS");
        mailSender.send(message);

        User user = userRepository.findByEmailAndIsDeletedFalse(to).orElse(null);
        Integer userId = user != null ? user.getId() : null;

        auditLogService.log("EMAIL_SENT", "USER", userId.longValue(), null, "Verification email sent with code: " + code);

    } catch (Exception e) {
        auditLogService.log("EMAIL_FAILED", "USER", null, null, "Failed to send email to " + to + ": " + e.getMessage());
        throw new RuntimeException("Failed to send verification email: " + e.getMessage());
    }
}
}
