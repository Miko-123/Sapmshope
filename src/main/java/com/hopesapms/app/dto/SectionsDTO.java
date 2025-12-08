package com.hopesapms.app.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SectionsDTO {
    private Integer id;
    private String name;
    private Integer yearLevel;
    private Long programId; // <-- changed to Long
    private String programName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
