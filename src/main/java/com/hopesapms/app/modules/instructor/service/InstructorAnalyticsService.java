package com.hopesapms.app.modules.instructor.service;

import com.hopesapms.app.modules.instructor.dto.InstructorAttendanceDashboardDTO;
import com.hopesapms.app.modules.instructor.dto.InstructorAttendanceDashboardDTO.*;
import com.hopesapms.app.modules.schedule.model.ClassSession;
import com.hopesapms.app.modules.department.model.Department;
import com.hopesapms.app.modules.schedule.repository.ClassSessionRepository;
import com.hopesapms.app.modules.department.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InstructorAnalyticsService {

    private final DepartmentRepository departmentRepository;
    private final ClassSessionRepository classSessionRepository;

    @Transactional(readOnly = true)
    public InstructorAttendanceDashboardDTO getDashboardData() {
        
        List<ClassSession> allSessions = classSessionRepository.findAllActiveSessionsWithDetails();
        List<Department> allDepartments = departmentRepository.findByIsDeletedFalse();

        long totalSessions = allSessions.size();
        long presentCount = allSessions.stream()
                .filter(s -> "PRESENT".equalsIgnoreCase(s.getInstructorStatus()))
                .count();

        double avgRate = totalSessions > 0 ? ((double) presentCount / totalSessions) * 100 : 0.0;

        Map<Long, List<ClassSession>> byInstructor = allSessions.stream()
                .collect(Collectors.groupingBy(s -> s.getCourseOffering().getInstructor().getId()));

        long totalFaculty = byInstructor.size();
        long perfectRecord = 0;
        long belowTarget = 0;

        for (List<ClassSession> instructorSessions : byInstructor.values()) {
            long p = instructorSessions.stream()
                    .filter(s -> "PRESENT".equalsIgnoreCase(s.getInstructorStatus()))
                    .count();
            double rate = ((double) p / instructorSessions.size()) * 100;
            
            if (rate >= 100.0) perfectRecord++;
            if (rate < 95.0) belowTarget++;
        }

        AttendanceMetricsDTO metrics = AttendanceMetricsDTO.builder()
                .averageAttendance(String.format("%.1f%%", avgRate))
                .totalFaculty(totalFaculty)
                .perfectAttendance(perfectRecord)
                .belowTarget(belowTarget)
                .build();

        Map<String, List<ClassSession>> byMonth = allSessions.stream()
                .collect(Collectors.groupingBy(s -> s.getSessionDate().getMonth()
                        .getDisplayName(TextStyle.SHORT, Locale.ENGLISH)));

        List<MonthlyTrendDTO> monthlyTrends = new ArrayList<>();
        String[] monthOrder = { "Aug", "Sep", "Oct", "Nov", "Dec", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul" };

        for (String month : monthOrder) {
            if (byMonth.containsKey(month)) {
                List<ClassSession> monthSessions = byMonth.get(month);
                long p = monthSessions.stream()
                        .filter(s -> "PRESENT".equalsIgnoreCase(s.getInstructorStatus()))
                        .count();
                double rate = ((double) p / monthSessions.size()) * 100;
                monthlyTrends.add(new MonthlyTrendDTO(month, Math.round(rate * 10.0) / 10.0));
            }
        }

        List<DeptSummaryDTO> deptSummaries = new ArrayList<>();
        List<DeptAttendanceDTO> deptCharts = new ArrayList<>();

        for (Department dept : allDepartments) {
            List<ClassSession> deptSessions = allSessions.stream()
                    .filter(s -> s.getCourseOffering().getInstructor().getUser().getDepartment().getId().equals(dept.getId()))
                    .collect(Collectors.toList());

            if (deptSessions.isEmpty()) continue;

            long deptPresent = deptSessions.stream()
                    .filter(s -> "PRESENT".equalsIgnoreCase(s.getInstructorStatus()))
                    .count();
            double deptRate = ((double) deptPresent / deptSessions.size()) * 100;

            Map<Long, List<ClassSession>> deptInstMap = deptSessions.stream()
                    .collect(Collectors.groupingBy(s -> s.getCourseOffering().getInstructor().getId()));

            long deptPerfect = 0;
            long deptBelow = 0;
            for (List<ClassSession> instRecs : deptInstMap.values()) {
                long ip = instRecs.stream().filter(s -> "PRESENT".equalsIgnoreCase(s.getInstructorStatus())).count();
                double ir = ((double) ip / instRecs.size()) * 100;
                if (ir >= 100) deptPerfect++;
                if (ir < 95) deptBelow++;
            }

            String status = deptRate >= 97 ? "Excellent" : (deptRate >= 95 ? "Good" : "Needs Review");

            deptSummaries.add(DeptSummaryDTO.builder()
                    .id(dept.getName())
                    .department(dept.getName())
                    .faculty((long) deptInstMap.size())
                    .avgAttendance(Math.round(deptRate * 10.0) / 10.0)
                    .perfectRecord(deptPerfect)
                    .belowTarget(deptBelow)
                    .status(status)
                    .build());

            deptCharts.add(new DeptAttendanceDTO(dept.getName(), Math.round(deptRate * 10.0) / 10.0));
        }

        List<ClassSession> absences = allSessions.stream()
                .filter(s -> s.getInstructorStatus() != null && !"PRESENT".equalsIgnoreCase(s.getInstructorStatus()))
                .collect(Collectors.toList());

        long totalAbsences = absences.size();
        Map<String, Long> reasonCounts = absences.stream()
                .collect(Collectors.groupingBy(ClassSession::getInstructorStatus, Collectors.counting()));

        List<AbsenceReasonDTO> reasonDTOs = reasonCounts.entrySet().stream()
                .map(e -> new AbsenceReasonDTO(
                        e.getKey(),
                        e.getValue(),
                        totalAbsences > 0 ? Math.round(((double) e.getValue() / totalAbsences) * 100) : 0))
                .collect(Collectors.toList());

        return InstructorAttendanceDashboardDTO.builder()
                .metrics(metrics)
                .monthlyTrend(monthlyTrends)
                .departmentAttendance(deptCharts)
                .departmentSummary(deptSummaries)
                .absenceReasons(reasonDTOs)
                .build();
    }
}