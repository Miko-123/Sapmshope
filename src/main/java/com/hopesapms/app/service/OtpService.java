package com.hopesapms.app.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.hopesapms.app.model.User;
import com.hopesapms.app.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
@Slf4j
@RequiredArgsConstructor
public class OtpService {

    private final AuditLogService auditLogService;
    private final UserRepository userRepository;

    private final Map<String, OtpData> otpCache = new HashMap<>();
    private final Random random = new Random();
    private static final int EXPIRATION_MINUTES = 5;

    public String generateAndCacheOtp(String email) {
        String otp = String.format("%06d", random.nextInt(999999));
        otpCache.put(email, new OtpData(otp, LocalDateTime.now().plusMinutes(EXPIRATION_MINUTES)));
        log.info("Generated OTP {} for {}", otp, email);

        User user = userRepository.findByEmailAndIsDeletedFalse(email).orElse(null);
        Integer userId = user != null ? user.getId() : null;

        auditLogService.log("OTP_REQUEST", "USER", userId.longValue(), null, "OTP generated: " + otp);
        return otp;
    }

    public boolean validateOtp(String email, String otp) {
        OtpData data = otpCache.get(email);
        User user = userRepository.findByEmailAndIsDeletedFalse(email).orElse(null);
        Integer userId = user != null ? user.getId() : null;

        if (data == null || data.expiration.isBefore(LocalDateTime.now())) {
            otpCache.remove(email);
            auditLogService.log("OTP_EXPIRED", "USER", userId.longValue(), null, "OTP expired or not found");
            return false;
        }

        boolean isValid = data.code.equals(otp);
        if (isValid) {
            otpCache.remove(email);
            auditLogService.log("OTP_VERIFY", "USER", userId.longValue(), null, "OTP verified successfully");
        } else {
            auditLogService.log("OTP_INVALID", "USER", userId.longValue(), null, "Invalid OTP attempt");
        }

        return isValid;
    }

    private record OtpData(String code, LocalDateTime expiration) {}
}