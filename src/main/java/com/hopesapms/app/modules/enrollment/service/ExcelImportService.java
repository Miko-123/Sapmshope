package com.hopesapms.app.modules.enrollment.service;

import com.hopesapms.app.modules.student.dto.RegisterStudentRequest;
import com.hopesapms.app.modules.student.dto.StudentResponse;
import com.hopesapms.app.modules.enrollment.dto.BulkEnrollmentRowDTO;
import com.hopesapms.app.modules.department.model.Department;
import com.hopesapms.app.modules.program.model.Program;
import com.hopesapms.app.modules.section.model.Section;
import com.hopesapms.app.modules.department.repository.DepartmentRepository;
import com.hopesapms.app.modules.program.repository.ProgramRepository;
import com.hopesapms.app.modules.section.repository.SectionRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import com.hopesapms.app.modules.student.service.StudentService;

@Service
@RequiredArgsConstructor
public class ExcelImportService {

    private final StudentService studentService;
    private final DepartmentRepository departmentRepository;
    private final ProgramRepository programRepository;
    private final SectionRepository sectionRepository;
    private final Validator validator;

    @Transactional(readOnly = true)
    public Map<String, Object> importStudents(MultipartFile file) {
        List<StudentResponse> successfulImports = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int rowNumber = 0;

        try (InputStream inputStream = file.getInputStream()) {
            Workbook workbook = new XSSFWorkbook(inputStream);
            Sheet sheet = workbook.getSheetAt(0);

            for (Row row : sheet) {
                rowNumber++;
                if (rowNumber == 1) continue; // Skip header

                if (row.getCell(0) == null || row.getCell(0).getCellType() == CellType.BLANK) continue;

                try {
                    DataFormatter formatter = new DataFormatter();
                    RegisterStudentRequest request = new RegisterStudentRequest();

                    request.setStudentId(formatter.formatCellValue(row.getCell(0)));
                    request.setFirstName(formatter.formatCellValue(row.getCell(1)));
                    request.setMiddleName(formatter.formatCellValue(row.getCell(2)));
                    request.setLastName(formatter.formatCellValue(row.getCell(3)));
                    request.setEmail(formatter.formatCellValue(row.getCell(4)));
                    String departmentName = formatter.formatCellValue(row.getCell(5));
                    String programName = formatter.formatCellValue(row.getCell(6));
                    Integer yearLevel = parseIntegerCell(row.getCell(7), "Year Level", rowNumber);
                    request.setYearLevel(yearLevel);
                    String sectionName = formatter.formatCellValue(row.getCell(8));

                    // Resolve names to IDs first
                    Department department = resolveDepartment(null, departmentName, rowNumber);
                    Program program = resolveProgram(null, programName, department, rowNumber);
                    Section section = resolveSection(null, sectionName, program, yearLevel, rowNumber);

                    request.setDepartmentId(department.getId());
                    request.setProgramId(program.getId());
                    request.setSectionId(section.getId().longValue());

                    // Validate after setting IDs
                    Set<ConstraintViolation<RegisterStudentRequest>> violations = validator.validate(request);
                    if (!violations.isEmpty()) {
                        String errorMsg = violations.stream()
                                .map(ConstraintViolation::getMessage)
                                .collect(Collectors.joining("; "));
                        throw new IllegalArgumentException(errorMsg);
                    }

                    StudentResponse response = studentService.registerStudent(request);
                    successfulImports.add(response);

                } catch (Exception e) {
                    errors.add("Row " + rowNumber + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            errors.add("Failed to parse Excel file: " + e.getMessage());
        }

        Map<String, Object> report = new HashMap<>();
        report.put("successCount", successfulImports.size());
        report.put("errorCount", errors.size());
        report.put("errors", errors);
        return report;
    }

    private Long parseLongCell(Cell cell) {
        if (cell == null)
            return null;
        if (cell.getCellType() == CellType.NUMERIC) {
            return (long) cell.getNumericCellValue();
        }
        String raw = new DataFormatter().formatCellValue(cell);
        return raw == null || raw.isBlank() ? null : Long.parseLong(raw.trim());
    }

    private Integer parseIntegerCell(Cell cell, String columnName, int rowNumber) {
        if (cell == null) {
            throw new IllegalArgumentException(columnName + " is required");
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            return (int) cell.getNumericCellValue();
        }
        String raw = new DataFormatter().formatCellValue(cell);
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException(columnName + " is required");
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid " + columnName + " at row " + rowNumber);
        }
    }

    private Department resolveDepartment(Long id, String name, int rowNumber) {
        if (id != null) {
            return departmentRepository.findByIdAndIsDeletedFalse(id)
                    .orElseThrow(() -> new IllegalArgumentException("Department ID " + id + " not found"));
        }
        if (name != null && !name.isBlank()) {
            return departmentRepository.findByNameIgnoreCaseAndIsDeletedFalse(name.trim())
                    .orElseThrow(() -> new IllegalArgumentException("Department '" + name + "' not found"));
        }
        throw new IllegalArgumentException("Row " + rowNumber + ": Department ID or Name is required");
    }

    private Program resolveProgram(Long id, String name, Department department, int rowNumber) {
        if (id != null) {
            Program program = programRepository.findByIdAndIsDeletedFalse(id)
                    .orElseThrow(() -> new IllegalArgumentException("Program ID " + id + " not found"));
            if (!program.getDepartment().getId().equals(department.getId())) {
                throw new IllegalArgumentException(
                        "Program ID " + id + " does not belong to Department " + department.getName());
            }
            return program;
        }
        if (name != null && !name.isBlank()) {
            return programRepository
                    .findByNameIgnoreCaseAndDepartmentIdAndIsDeletedFalse(name.trim(), department.getId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Program '" + name + "' not found under Department " + department.getName()));
        }
        throw new IllegalArgumentException("Row " + rowNumber + ": Program ID or Name is required");
    }

    private Section resolveSection(Long id, String name, Program program, Integer yearLevel, int rowNumber) {
        if (id != null) {
            Section section = sectionRepository.findByIdAndIsDeletedFalse(id.intValue())
                    .orElseThrow(() -> new IllegalArgumentException("Section ID " + id + " not found"));
            if (!section.getProgram().getId().equals(program.getId()) ||
                !Objects.equals(section.getYearLevel(), yearLevel)) {

                throw new IllegalArgumentException(
                        "Section ID " + id + " does not belong to Program " +
                        program.getName() + " (Year " + yearLevel + ")"
                );
            }
            return section;
        }

        if (name != null && !name.isBlank()) {
            return sectionRepository
                    .findByNameIgnoreCaseAndProgramIdAndYearLevelAndIsDeletedFalse(
                            name.trim(),
                            program.getId(),
                            yearLevel
                    )
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Section '" + name + "' not found under Program " +
                            program.getName() + " (Year " + yearLevel + ")"
                    ));
        }

        throw new IllegalArgumentException(
                "Row " + rowNumber + ": Section ID or Name is required"
        );
    }

    public List<BulkEnrollmentRowDTO> parseEnrollments(MultipartFile file) {
        List<BulkEnrollmentRowDTO> rows = new ArrayList<>();
        int rowNumber = 0;

        try (InputStream inputStream = file.getInputStream()) {
            Workbook workbook = new XSSFWorkbook(inputStream);
            Sheet sheet = workbook.getSheetAt(0);

            for (Row row : sheet) {
                rowNumber++;
                if (rowNumber == 1) continue;

                try {
                    DataFormatter formatter = new DataFormatter();
                    String studentId = formatter.formatCellValue(row.getCell(0));
                    String courseOfferingStr = formatter.formatCellValue(row.getCell(1));

                    if ((studentId == null || studentId.isBlank()) &&
                            (courseOfferingStr == null || courseOfferingStr.isBlank())) {
                        continue; // skip empty rows
                    }

                    Long courseOfferingId = null;
                    if (courseOfferingStr != null && !courseOfferingStr.isBlank()) {
                        courseOfferingId = courseOfferingStr.contains(".")
                                ? (long) Double.parseDouble(courseOfferingStr)
                                : Long.parseLong(courseOfferingStr);
                    }

                    if (studentId == null || studentId.isBlank() || courseOfferingId == null) {
                        continue;
                    }

                    rows.add(new BulkEnrollmentRowDTO(studentId, courseOfferingId));

                } catch (Exception e) {
                    throw new RuntimeException("Row " + rowNumber + ": Failed to parse. " + e.getMessage());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Excel file: " + e.getMessage());
        }
        return rows;
    }
}