package com.hopesapms.app.service;

import com.hopesapms.app.dto.RegisterStudentRequest;
import com.hopesapms.app.dto.StudentResponse;
import com.hopesapms.app.dto.BulkEnrollmentRowDTO;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ExcelImportService {

    private final StudentService studentService;

    public Map<String, Object> importStudents(MultipartFile file) {
        List<StudentResponse> successfulImports = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int rowNumber = 0;

        try (InputStream inputStream = file.getInputStream()) {
            Workbook workbook = new XSSFWorkbook(inputStream);
            Sheet sheet = workbook.getSheetAt(0);

            for (Row row : sheet) {
                rowNumber++;
                if (rowNumber == 1) continue; 

                try {
                    RegisterStudentRequest request = new RegisterStudentRequest();
                    DataFormatter formatter = new DataFormatter();

                    request.setStudentId(formatter.formatCellValue(row.getCell(0)));
                    request.setFirstName(formatter.formatCellValue(row.getCell(1)));
                    request.setMiddleName(formatter.formatCellValue(row.getCell(2)));
                    request.setLastName(formatter.formatCellValue(row.getCell(3)));
                    request.setEmail(formatter.formatCellValue(row.getCell(4)));

                    request.setDepartmentId((long) row.getCell(5).getNumericCellValue());
                    request.setProgramId((long) row.getCell(6).getNumericCellValue());
                    request.setYearLevel((int) row.getCell(7).getNumericCellValue());
                    request.setSectionId((long) row.getCell(8).getNumericCellValue());

                    StudentResponse response = studentService.registerStudent(request);
                    successfulImports.add(response);

                } catch (Exception e) {
                    errors.add("Row " + rowNumber + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Excel file: " + e.getMessage());
        }

        Map<String, Object> report = new HashMap<>();
        report.put("successCount", successfulImports.size());
        report.put("errorCount", errors.size());
        report.put("errors", errors);
        return report;
    }

    public List<BulkEnrollmentRowDTO> parseEnrollments(MultipartFile file) {
        List<BulkEnrollmentRowDTO> rows = new ArrayList<>();
        int rowNumber = 0;

        try (InputStream inputStream = file.getInputStream()) {
            Workbook workbook = new XSSFWorkbook(inputStream);
            Sheet sheet = workbook.getSheetAt(0);

            for (Row row : sheet) {
                rowNumber++;
                if (rowNumber == 1) continue; // Skip Header

                try {
                    DataFormatter formatter = new DataFormatter();

                    // Column 0: studentId (e.g., "RCS/007/22")
                    String studentId = formatter.formatCellValue(row.getCell(0));

                    // Column 1: courseOfferingId (e.g., 1)
                    String courseOfferingStr = formatter.formatCellValue(row.getCell(1));
                    if ((studentId == null || studentId.isBlank()) && (courseOfferingStr == null || courseOfferingStr.isBlank())) {
                        // Skip completely empty rows
                        continue;
                    }

                    Long courseOfferingId = null;
                    if (courseOfferingStr != null && !courseOfferingStr.isBlank()) {
                        try {
                            if (courseOfferingStr.contains(".")) {
                                courseOfferingId = (long) Double.parseDouble(courseOfferingStr);
                            } else {
                                courseOfferingId = Long.parseLong(courseOfferingStr);
                            }
                        } catch (NumberFormatException nfe) {
                            throw new RuntimeException("Row " + rowNumber + ": Invalid courseOfferingId '" + courseOfferingStr + "'.");
                        }
                    }

                    if (studentId == null || studentId.isBlank() || courseOfferingId == null) {
                        // Skip rows missing required values
                        continue;
                    }

                    rows.add(new BulkEnrollmentRowDTO(studentId, courseOfferingId));

                } catch (Exception e) {
                    // Throw an error with the specific row for better debugging
                    throw new RuntimeException("Row " + rowNumber + ": Failed to parse. " + e.getMessage());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Excel file: " + e.getMessage());
        }
        return rows;
    }
}