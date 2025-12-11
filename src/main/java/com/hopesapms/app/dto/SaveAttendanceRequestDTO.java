package com.hopesapms.app.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class SaveAttendanceRequestDTO {
    private Long courseOfferingId;
    private LocalDate date;
    private List<StudentEntry> records;

    @Data
    public static class StudentEntry {
        private Integer studentId;
        private String status;
        private String remarks;
    }
}