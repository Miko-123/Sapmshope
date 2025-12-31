package com.hopesapms.app.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "login_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email; 

    private Integer userId; 

    @Column(nullable = false)
    private LocalDateTime timestamp;

    private boolean isSuccess;

    private String ipAddress;
    
    private String userAgent;

    private String failureReason;
}