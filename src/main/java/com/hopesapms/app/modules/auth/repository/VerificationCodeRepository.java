package com.hopesapms.app.modules.auth.repository;

import com.hopesapms.app.modules.auth.model.VerificationCode;
import com.hopesapms.app.modules.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VerificationCodeRepository extends JpaRepository<VerificationCode, Long> {
     Optional<VerificationCode> findByUserAndCodeAndUsedFalse(User user, String code);
} 