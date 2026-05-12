package com.hopesapms.app.modules.academicsemester.dto;

import lombok.Data;

@Data
public class SemesterRolloverRequest {
    private Long sourceSemesterId;
    private Long targetSemesterId;
    private boolean copyInstructors;
}