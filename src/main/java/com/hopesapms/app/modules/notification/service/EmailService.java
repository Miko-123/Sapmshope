package com.hopesapms.app.modules.notification.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.hopesapms.app.modules.user.model.User;
import com.hopesapms.app.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;

import com.hopesapms.app.modules.auditlog.service.AuditLogService;

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

  public void sendPasswordResetEmail(String to, String resetLink) {
    try {
      MimeMessage mimeMessage = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);

      helper.setTo(to);
      helper.setSubject("Reset Your Password - HOPE SAPMS");

      String htmlContent = """
          <html>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
              <div style="max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #eee; border-radius: 8px;">
                <h2 style="color: #2c3e50;">Password Reset Request</h2>
                <p>Hello,</p>
                <p>We received a request to reset your password for your HOPE SAPMS account. If you didn't make this request, you can safely ignore this email.</p>
                <p>To reset your password, click the button below:</p>
                <div style="text-align: center; margin: 30px 0;">
                  <a href="%s" style="background-color: #3498db; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px; font-weight: bold;">Reset Password</a>
                </div>
                <p style="font-size: 13px; color: #7f8c8d;">This link will expire in 15 minutes.</p>
                <p style="margin-top: 20px;">Regards,<br/>HOPE SAPMS Team</p>
              </div>
            </body>
          </html>
          """
          .formatted(resetLink);

      helper.setText(htmlContent, true);

      mailSender.send(mimeMessage);

      User user = userRepository.findByEmailAndIsDeletedFalse(to).orElse(null);
      Long userId = user != null ? user.getId().longValue() : null;

      auditLogService.log("EMAIL_SENT", "USER", userId, null,
          "Password reset email sent to " + to);

    } catch (MessagingException e) {
      auditLogService.log("EMAIL_FAILED", "USER", null, null,
          "Failed to send password reset email to " + to + ": " + e.getMessage());
      throw new RuntimeException("Failed to send password reset email: " + e.getMessage());
    }
  }
}
