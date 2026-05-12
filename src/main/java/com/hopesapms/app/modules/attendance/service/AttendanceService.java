package com.hopesapms.app.modules.attendance.service;

import com.hopesapms.app.modules.attendance.dto.AttendanceSheetDTO;
import com.hopesapms.app.modules.attendance.dto.SaveAttendanceRequestDTO;
import com.hopesapms.app.common.exception.ResourceNotFoundException;
import com.hopesapms.app.modules.attendance.model.Attendance;
import com.hopesapms.app.modules.schedule.model.ClassSession;
import com.hopesapms.app.modules.courseoffering.model.CourseOffering;
import com.hopesapms.app.modules.enrollment.model.Enrollment;
import com.hopesapms.app.modules.student.model.Student;
import com.hopesapms.app.modules.attendance.repository.AttendanceRepository;
import com.hopesapms.app.modules.schedule.repository.ClassSessionRepository;
import com.hopesapms.app.modules.courseoffering.repository.CourseOfferingRepository;
import com.hopesapms.app.modules.enrollment.repository.EnrollmentRepository;
import com.hopesapms.app.modules.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import com.hopesapms.app.common.event.AppEvents;
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
    private final ApplicationEventPublisher eventPublisher;

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
            row.setStudentName(
                    s.getUser().getFirstName() + " " + s.getUser().getMiddleName() + " " + s.getUser().getLastName());
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

            if ("ABSENT".equals(entry.getStatus())) {
                checkAndApplyNGStatus(student, offering);
                checkAndNotifyAtRisk(student, offering);
            }
        }
    }

    private void checkAndNotifyAtRisk(Student student, CourseOffering offering) {
        List<Attendance> records = attendanceRepository.findByStudentAndCourse(student.getId(), offering.getId());
        long total = records.size();
        long present = records.stream().filter(a -> "PRESENT".equals(a.getStatus())).count();

        if (total > 0) {
            double rate = (double) present / total * 100.0;
            if (rate < 80.0) {
                eventPublisher.publishEvent(new AppEvents.LowAttendanceEvent(
                        this,
                        student.getUser(),
                        offering.getCourse().getTitle(),
                        rate));
            }
        }
    }

    private void checkAndApplyNGStatus(Student student, CourseOffering offering) {
        List<Attendance> records = attendanceRepository.findByStudentAndCourse(student.getId(), offering.getId());

        long totalSessions = records.size();
        long absentSessions = records.stream()
                .filter(a -> "ABSENT".equals(a.getStatus()))
                .count();

        if (totalSessions > 0) {
            double absencePercentage = (double) absentSessions / totalSessions * 100.0;

            if (absencePercentage >= 25.0) {
                Enrollment enrollment = enrollmentRepository
                        .findByStudentAndCourseOffering(student.getId(), offering.getId())
                        .orElse(null);

                if (enrollment != null && !"NG".equals(enrollment.getFinalGrade())) {
                    enrollment.setFinalGrade("NG");
                    enrollmentRepository.save(enrollment);

                    System.out.println("System Auto-NG: Student " + student.getStudentId() +
                            " hit " + absencePercentage + "% absence.");
                }
            }
        }
    }
}