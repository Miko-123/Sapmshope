package com.hopesapms.app.service;

import com.hopesapms.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import java.time.LocalDateTime;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import com.hopesapms.app.model.LoginLog;
import com.hopesapms.app.repository.LoginLogRepository;
import com.hopesapms.app.model.User;
import org.springframework.transaction.annotation.Propagation; 
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LoginLogService {
    private final LoginLogRepository loginLogRepository;
    private final UserRepository userRepository; 

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logLogin(String email, boolean success, String ip, String userAgent, String failureReason) {
        
        Integer userId = null;
        
        if (success) {
            Optional<User> user = userRepository.findByEmailAndIsDeletedFalse(email); 
            if (user.isPresent()) {
                userId = user.get().getId();
            }
        }

        LoginLog log = LoginLog.builder()
                .email(email) 
                .userId(userId)
                .timestamp(LocalDateTime.now())
                .isSuccess(success)
                .ipAddress(ip)
                .userAgent(userAgent)
                .failureReason(failureReason)
                .build();
        
        loginLogRepository.save(log);
    }
    
    public Page<LoginLog> getLoginHistory(Pageable pageable) {
        return loginLogRepository.findAll(pageable);
    }
}