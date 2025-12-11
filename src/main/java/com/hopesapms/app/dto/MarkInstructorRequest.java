package com.hopesapms.app.dto;

import lombok.Data;

@Data
public class MarkInstructorRequest {
    private Long courseOfferingId;
    private String status; 
    private String remarks;
}