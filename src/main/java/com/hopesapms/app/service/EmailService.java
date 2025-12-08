package com.hopesapms.app.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
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
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);

            helper.setTo(to);
            helper.setSubject("Your Verification Code");

            String htmlContent = """
                <html>
                  <body style="font-family: Arial, sans-serif; line-height: 1.6;">
                    <h2 style="color: #2c3e50;">Hello,</h2>
                    <p>Your verification code is:</p>
                    <div style="padding: 10px; background-color: #f4f4f4; border-radius: 5px; display: inline-block;">
                      <strong style="font-size: 18px; color: #e74c3c;">%s</strong>
                    </div>
                    <p style="margin-top: 20px;">Regards,<br/>HOPE SAPMS</p>
                  </body>
                </html>
                """.formatted(code);

            helper.setText(htmlContent, true); 

            mailSender.send(mimeMessage);

            User user = userRepository.findByEmailAndIsDeletedFalse(to).orElse(null);
            Integer userId = user != null ? user.getId() : null;

            if (userId != null) {
                auditLogService.log("EMAIL_SENT", "USER", userId.longValue(), null,
                        "Verification email sent with code: " + code);
            } else {
                auditLogService.log("EMAIL_SENT", "USER", null, null,
                        "Verification email sent with code: " + code);
            }

        } catch (MessagingException e) {
            auditLogService.log("EMAIL_FAILED", "USER", null, null,
                    "Failed to send email to " + to + ": " + e.getMessage());
            throw new RuntimeException("Failed to send verification email: " + e.getMessage());
        }
    }

}
