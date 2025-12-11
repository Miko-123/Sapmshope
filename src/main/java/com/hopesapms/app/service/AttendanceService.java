package com.hopesapms.app.service;

import com.hopesapms.app.dto.*;
import com.hopesapms.app.exception.ResourceNotFoundException;
import com.hopesapms.app.model.*;
import com.hopesapms.app.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final ClassSessionRepository classSessionRepository;
    private final AttendanceRepository attendanceRepository;
    private final CourseOfferingRepository courseOfferingRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;

    @Transactional(readOnly = true)
    public AttendanceSheetDTO getSheet(Long offeringId, LocalDate date) {
        ClassSession session = classSessionRepository.findByCourseOfferingIdAndSessionDate(offeringId, date)
                .orElse(null);

        AttendanceSheetDTO dto = new AttendanceSheetDTO();
        dto.setDate(date);

        Map<Integer, Attendance> attendanceMap;

        if (session != null) {
            dto.setSessionId(session.getId());
            attendanceMap = attendanceRepository.findByClassSessionId(session.getId()).stream()
                    .collect(Collectors.toMap(a -> a.getStudent().getId(), a -> a));
        } else {
            attendanceMap = Map.of();
        }

        List<Enrollment> enrollments = enrollmentRepository.findByCourseOfferingId(offeringId);

        List<AttendanceSheetDTO.StudentStatusDTO> rows = new ArrayList<>();
        for (Enrollment e : enrollments) {
            Student s = e.getStudent();
            AttendanceSheetDTO.StudentStatusDTO row = new AttendanceSheetDTO.StudentStatusDTO();
            row.setStudentId(s.getId());
            row.setStudentName(s.getUser().getFirstName() + " " + s.getUser().getLastName());
            row.setStudentIdString(s.getStudentId());

            if (attendanceMap.containsKey(s.getId())) {
                Attendance existing = attendanceMap.get(s.getId());
                row.setStatus(existing.getStatus());
                row.setRemarks(existing.getRemarks());
            } else {
                row.setStatus("PRESENT");
            }
            rows.add(row);
        }
        dto.setStudents(rows);
        return dto;
    }

    @Transactional
    public void saveAttendance(SaveAttendanceRequestDTO dto) {
        CourseOffering offering = courseOfferingRepository.findById(dto.getCourseOfferingId())
                .orElseThrow(() -> new ResourceNotFoundException("Offering not found"));

        ClassSession session = classSessionRepository
                .findByCourseOfferingIdAndSessionDate(dto.getCourseOfferingId(), dto.getDate())
                .orElse(ClassSession.builder()
                        .courseOffering(offering)
                        .sessionDate(dto.getDate())
                        .startTime(LocalTime.now())
                        .status("COMPLETED")
                        .build());
        session = classSessionRepository.save(session);

        for (SaveAttendanceRequestDTO.StudentEntry entry : dto.getRecords()) {
            Student student = studentRepository.findById(entry.getStudentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

            Attendance attendance = null;
            List<Attendance> existingList = attendanceRepository.findByClassSessionId(session.getId());

            for (Attendance a : existingList) {
                if (a.getStudent().getId().equals(student.getId())) {
                    attendance = a;
                    break;
                }
            }

            if (attendance == null) {
                attendance = new Attendance();
                attendance.setClassSession(session);
                attendance.setStudent(student);
            }

            attendance.setStatus(entry.getStatus());
            attendance.setRemarks(entry.getRemarks());
            attendanceRepository.save(attendance);
        }
    }
}