package com.hopesapms.app.dto;

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