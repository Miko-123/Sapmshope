package com.hopesapms.app.repository;

import com.hopesapms.app.model.LoginLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LoginLogRepository extends JpaRepository<LoginLog, Long> {
    
    Page<LoginLog> findByEmailContainingIgnoreCase(String email, Pageable pageable);
    
    Page<LoginLog> findByIsSuccess(boolean isSuccess, Pageable pageable);
}