package com.hopesapms.app.modules.attendance.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class AttendanceSheetDTO {
    private Integer sessionId;
    private LocalDate date;
    private List<StudentStatusDTO> students;

    @Data
    public static class StudentStatusDTO {
        private Integer studentId;
        private String studentName;
        private String studentIdString;
        private String status;
        private String remarks;
    }
}