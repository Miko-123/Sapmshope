package com.hopesapms.app.service;

import com.hopesapms.app.exception.ResourceNotFoundException;
import com.hopesapms.app.model.*;
import com.hopesapms.app.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourseOfferingImportService {
    
    private final CourseRepository courseRepository;
    private final SectionRepository sectionRepository;
    private final UserRepository userRepository;
    private final InstructorRepository instructorRepository;
    private final AcademicSemesterRepository academicSemesterRepository;
    private final CourseOfferingRepository courseOfferingRepository;
    private final AuditLogService auditLogService;

    private Map<String, CourseOffering> offeringCache;
    private List<String> errors;
    private int offeringsCreated;
    private int slotsAdded;

    @Transactional
    public Map<String, Object> importOfferings(MultipartFile file, String defaultStatus) {
        this.offeringCache = new HashMap<>();
        this.errors = new ArrayList<>();
        this.offeringsCreated = 0;
        this.slotsAdded = 0;

        try (InputStream is = file.getInputStream()) {
            Workbook workbook = new XSSFWorkbook(is);
            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                
                try {
                    processRow(row, defaultStatus);
                } catch (Exception e) {
                    log.error("Error processing row {}: {}", i + 1, e.getMessage());
                    errors.add("Row " + (i + 1) + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse Excel file", e);
            errors.add("Failed to parse Excel file: " + e.getMessage());
        }

        auditLogService.log("IMPORT_COURSE_OFFERINGS", "System", null, null, 
            "Created: " + offeringsCreated + ", Slots Added: " + slotsAdded + ", Errors: " + errors.size());

        Map<String, Object> result = new HashMap<>();
        result.put("offeringsCreated", offeringsCreated);
        result.put("scheduleSlotsAdded", slotsAdded);
        result.put("errors", errors);
        return result;
    }

    private void processRow(Row row, String status) {
        String courseCode = getCellStringValue(row, 0);
        String sectionName = getCellStringValue(row, 1);
        String instructorEmail = getCellStringValue(row, 2);
        String day = getCellStringValue(row, 3);
        String periods = getCellStringValue(row, 4);
        String room = getCellStringValue(row, 5);
        String semesterName = getCellStringValue(row, 6);
        
        if (courseCode.isBlank() || sectionName.isBlank() || instructorEmail.isBlank() || semesterName.isBlank()) {
            throw new IllegalArgumentException("Missing required data (CourseCode, Section, Email, or Semester)");
        }

        Course course = courseRepository.findByCourseCodeAndIsDeletedFalse(courseCode)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + courseCode));
        
        if (course.getProgram() == null) {
            throw new IllegalArgumentException("Course " + courseCode + " is not linked to a Program.");
        }
        if (course.getYearLevel() == null) {
            throw new IllegalArgumentException("Course " + courseCode + " is not linked to a Year Level.");
        }

        Section section = sectionRepository.findByNameAndProgramIdAndYearLevelAndIsDeletedFalse(
                    sectionName, 
                    course.getProgram().getId(), 
                    course.getYearLevel()
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Section '" + sectionName + "' not found for Program '" + 
                    course.getProgram().getName() + "' and Year " + course.getYearLevel()
                ));
                
        User user = userRepository.findByEmailAndIsDeletedFalse(instructorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + instructorEmail));
                
        Instructor instructor = instructorRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Instructor profile not found for user: " + instructorEmail));
        
        AcademicSemester semester = academicSemesterRepository.findByNameAndIsDeletedFalse(semesterName)
                .orElseThrow(() -> new ResourceNotFoundException("Semester not found: " + semesterName));
        
        String offeringKey = course.getId() + ":" + section.getId() + ":" + instructor.getId() + ":" + semester.getId();

        CourseOffering offering;
        if (offeringCache.containsKey(offeringKey)) {
            offering = offeringCache.get(offeringKey);
        } else {
            offering = CourseOffering.builder()
                    .course(course)
                    .section(section)
                    .instructor(instructor)
                    .academicSemester(semester)
                    .status(status)
                    .build();
            
            courseOfferingRepository.save(offering);
            offeringCache.put(offeringKey, offering);
            this.offeringsCreated++;
        }

        if (day.isBlank() || periods.isBlank()) {
            return; 
        }
        offering.addScheduleSlot(day, periods, room);
        this.slotsAdded++;
    }

    private String getCellStringValue(Row row, int cellIndex) {
        if (row.getCell(cellIndex) == null) {
            return "";
        }
        return row.getCell(cellIndex).getStringCellValue().trim();
    }

}
