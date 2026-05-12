package com.hopesapms.app.modules.enrollment.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkEnrollmentRowDTO {
    
    private String studentId;

    private Long courseOfferingId;
}