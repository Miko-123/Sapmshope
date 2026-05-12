package com.hopesapms.app.modules.enrollment.dto;

import lombok.Data;

@Data
public class BulkEnrollmentRequestDTO {
    
    private Long sectionId;
    private Long semesterId;
}
