package com.hopesapms.app.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailService {
    
    private final JavaMailSender mailSender;

    public void sendVerificationEmail(String to, String code) {
        try{
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Your Verification Code");
        message.setText("Hello,\n\nYour verification code is: " + code + "\n\nRegards,\nHOPE SAPMS");
        mailSender.send(message);
        } catch (Exception e){
            e.printStackTrace();
            throw new RuntimeException("Failed to send verification email: " + e.getMessage());
        }
    }
}
