package com.hopesapms.app.modules.report.service;

import com.hopesapms.app.modules.assessment.model.Assessment;
import com.hopesapms.app.modules.attendance.model.Attendance;
import com.hopesapms.app.modules.course.model.Course;
import com.hopesapms.app.modules.courseoffering.model.CourseOffering;
import com.hopesapms.app.modules.department.model.Department;
import com.hopesapms.app.modules.enrollment.model.Enrollment;
import com.hopesapms.app.modules.grading.model.GradingScale;
import com.hopesapms.app.modules.instructor.model.Instructor;
import com.hopesapms.app.modules.instructor.model.InstructorAttendance;
import com.hopesapms.app.modules.program.model.Program;
import com.hopesapms.app.modules.score.model.Score;
import com.hopesapms.app.modules.section.model.Section;
import com.hopesapms.app.modules.student.model.Student;
import com.hopesapms.app.modules.user.model.User;
import com.hopesapms.app.modules.attendance.repository.AttendanceRepository;
import com.hopesapms.app.modules.courseoffering.repository.CourseOfferingRepository;
import com.hopesapms.app.modules.enrollment.repository.EnrollmentRepository;
import com.hopesapms.app.modules.grading.repository.GradingScaleRepository;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import com.hopesapms.app.modules.instructor.repository.InstructorAttendanceRepository;
import com.hopesapms.app.modules.instructor.repository.InstructorRepository;
import com.hopesapms.app.modules.department.dto.DepartmentAttendanceReportDTO;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final GradingScaleRepository gradingScaleRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final InstructorRepository instructorRepository;
    private final InstructorAttendanceRepository instructorAttendanceRepository;
    private final AttendanceRepository studentAttendanceRepository;
    private final CourseOfferingRepository courseOfferingRepository;

    public byte[] generateCourseGradeReport(CourseOffering course, List<Enrollment> enrollments,
            List<Assessment> assessments, List<Score> allScores) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, out);
            document.open();

            Font fontHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            Font fontSubHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            Font fontNormal = FontFactory.getFont(FontFactory.HELVETICA, 9);

            Paragraph uniName = new Paragraph("HOPE ENTERPRISE UNIVERSITY COLLEGE", fontHeader);
            uniName.setAlignment(Element.ALIGN_CENTER);
            document.add(uniName);

            Paragraph office = new Paragraph("OFFICE OF THE REGISTRAR - GRADE SUBMISSION LIST", fontSubHeader);
            office.setAlignment(Element.ALIGN_CENTER);
            document.add(office);
            document.add(new Paragraph("\n"));

            PdfPTable metaTable = new PdfPTable(4);
            metaTable.setWidthPercentage(100);
            metaTable.setWidths(new float[] { 1, 2, 1, 2 });

            addMetaCell(metaTable, "Department:", course.getCourse().getProgram().getDepartment().getName(),
                    fontNormal);
            addMetaCell(metaTable, "Program:", "Regular/Degree", fontNormal);
            addMetaCell(metaTable, "Course Title:", course.getCourse().getTitle(), fontNormal);
            addMetaCell(metaTable, "Course Code:", course.getCourse().getCourseCode(), fontNormal);
            addMetaCell(metaTable, "Section:", course.getSection().getName(), fontNormal);
            addMetaCell(metaTable, "Academic Year:", LocalDate.now().getYear() + "/" + (LocalDate.now().getYear() + 1),
                    fontNormal);

            document.add(metaTable);
            document.add(new Paragraph("\n"));

            int fixedCols = 4;
            int assessCols = assessments.size();
            int endCols = 2;

            PdfPTable table = new PdfPTable(fixedCols + assessCols + endCols);
            table.setWidthPercentage(100);

            addHeaderCell(table, "No");
            addHeaderCell(table, "Student Name");
            addHeaderCell(table, "ID No.");
            addHeaderCell(table, "Sex");

            for (Assessment a : assessments) {

                double weightPercent = a.getWeight().multiply(BigDecimal.valueOf(100)).doubleValue();
                String label = a.getName() + "\n(" + String.format("%.0f", weightPercent) + "%)";
                addHeaderCell(table, label);
            }

            addHeaderCell(table, "Total\n(100%)");
            addHeaderCell(table, "Grade");

            int count = 1;

            Map<Integer, Map<Integer, Double>> scoreMap = mapScores(allScores);

            for (Enrollment e : enrollments) {
                Student s = e.getStudent();
                User u = s.getUser();

                addCell(table, String.valueOf(count++));
                addCell(table, u.getFirstName() + " " + u.getMiddleName() + " " + u.getLastName());
                addCell(table, s.getStudentId());
                addCell(table, u.getGender() != null ? u.getGender().name().substring(0, 1) : "-");

                Map<Integer, Double> studentScores = scoreMap.getOrDefault(e.getId(), Map.of());
                double totalCalc = 0;

                for (Assessment a : assessments) {
                    Double val = studentScores.get(a.getId());
                    if (val != null) {
                        addCell(table, String.valueOf(val));
                        totalCalc += val;
                    } else {
                        addCell(table, "-");
                    }
                }

                String finalGrade;
                if (e.getFinalGrade() != null) {

                    try {
                        finalGrade = getLetterGrade(Double.parseDouble(e.getFinalGrade()));
                    } catch (NumberFormatException ex) {
                        finalGrade = e.getFinalGrade();
                    }
                } else {
                    finalGrade = getLetterGrade(totalCalc);
                }

                addCell(table, String.format("%.1f", totalCalc));
                addCell(table, finalGrade);
            }

            document.add(table);
            document.add(new Paragraph("\n"));

            PdfPTable footerTable = new PdfPTable(3);
            footerTable.setWidthPercentage(100);
            footerTable.getDefaultCell().setBorder(Rectangle.NO_BORDER);

            String instructorName = "TBD";
            if (course.getInstructor() != null && course.getInstructor().getUser() != null) {
                instructorName = course.getInstructor().getUser().getFirstName() + " "
                        + course.getInstructor().getUser().getMiddleName() + " "
                        + course.getInstructor().getUser().getLastName();
            }

            addFooterCell(footerTable,
                    "Instructor:\n" + instructorName + "\n\nSig: ______________\nDate: ______________");
            addFooterCell(footerTable,
                    "Department Head:\n__________________\n\nSig: ______________\nDate: ______________");
            addFooterCell(footerTable,
                    "Registrar Office:\n__________________\n\nSig: ______________\nDate: ______________");

            document.add(footerTable);
            document.close();
            return out.toByteArray();
        }
    }

    private void addMetaCell(PdfPTable table, String label, String value, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(label + " " + value, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(3);
        table.addCell(cell);
    }

    private void addHeaderCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8)));
        cell.setBackgroundColor(Color.LIGHT_GRAY);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(4);
        table.addCell(cell);
    }

    private void addCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA, 8)));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(3);
        table.addCell(cell);
    }

    private void addFooterCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA, 9)));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingTop(20);
        table.addCell(cell);
    }

    private Map<Integer, Map<Integer, Double>> mapScores(List<Score> scores) {
        return scores.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getEnrollment().getId(),
                        Collectors.toMap(
                                s -> s.getAssessment().getId(),
                                s -> s.getScoreValue().doubleValue())));
    }

    private String getLetterGrade(double score) {
        return gradingScaleRepository.findByScore(score)
                .map(GradingScale::getLetterGrade)
                .orElse("-");
    }

    public byte[] generateStudentTranscript(Student student, List<Enrollment> enrollments) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

            Paragraph title = new Paragraph("HOPE ENTERPRISE UNIVERSITY COLLEGE", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Paragraph subtitle = new Paragraph("OFFICIAL STUDENT ACADEMIC RECORD", headerFont);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            document.add(subtitle);
            document.add(new Paragraph("\n"));

            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);

            addInfoRow(infoTable, "Student Name:",
                    student.getUser().getFirstName() + " " + student.getUser().getLastName());
            addInfoRow(infoTable, "Student ID:", student.getStudentId());
            addInfoRow(infoTable, "Department:", student.getDepartment().getName());
            addInfoRow(infoTable, "Program:", student.getProgram().getName());
            addInfoRow(infoTable, "Admission Year:", student.getEnrollmentDate().toString());

            document.add(infoTable);
            document.add(new Paragraph("\n"));

            Map<String, List<Enrollment>> semesterMap = enrollments.stream()
                    .collect(Collectors.groupingBy(e -> e.getCourseOffering().getAcademicSemester().getName()));

            for (String semester : semesterMap.keySet()) {
                PdfPTable semesterHeader = new PdfPTable(1);
                semesterHeader.setWidthPercentage(100);
                PdfPCell headerCell = new PdfPCell(new Phrase(semester, headerFont));
                headerCell.setBackgroundColor(Color.LIGHT_GRAY);
                semesterHeader.addCell(headerCell);
                document.add(semesterHeader);

                PdfPTable courseTable = new PdfPTable(5);
                courseTable.setWidthPercentage(100);
                courseTable.setWidths(new float[] { 2, 5, 1, 1, 1 });

                addHeaderCell(courseTable, "Code");
                addHeaderCell(courseTable, "Course Title");
                addHeaderCell(courseTable, "Cr");
                addHeaderCell(courseTable, "Gr");
                addHeaderCell(courseTable, "Pts");

                List<Enrollment> semCourses = semesterMap.get(semester);

                for (Enrollment e : semCourses) {
                    addCell(courseTable, e.getCourseOffering().getCourse().getCourseCode());
                    addCell(courseTable, e.getCourseOffering().getCourse().getTitle());

                    double credits = e.getCourseOffering().getCourse().getCredits();
                    addCell(courseTable, String.valueOf(credits));

                    String grade = "-";
                    double points = 0.0;

                    if (e.getFinalGrade() != null) {
                        grade = e.getFinalGrade();

                    }

                    addCell(courseTable, grade);
                    addCell(courseTable, "-");
                }
                document.add(courseTable);
                document.add(new Paragraph("\n"));
            }

            Paragraph footer = new Paragraph("This transcript is official only when bearing the university seal.",
                    FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8));
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();
            return out.toByteArray();
        }
    }

    private void addInfoRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPadding(3);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, FontFactory.getFont(FontFactory.HELVETICA, 10)));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPadding(3);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    public Map<String, Object> getDepartmentAnalytics(Long deptId) {

        List<Map<String, Object>> gradeDist = enrollmentRepository.findGradeDistributionByDepartment(deptId);

        long passCount = gradeDist.stream()
                .filter(m -> !m.get("label").equals("F"))
                .mapToLong(m -> (Long) m.get("value"))
                .sum();

        long failCount = gradeDist.stream()
                .filter(m -> m.get("label").equals("F"))
                .mapToLong(m -> (Long) m.get("value"))
                .sum();

        return Map.of(
                "gradeDistribution", gradeDist,
                "passRate", Map.of("Pass", passCount, "Fail", failCount));
    }

    public List<Map<String, Object>> getRegistrarEnrollmentTrends() {
        return enrollmentRepository.findEnrollmentTrends();
    }

    @Transactional(readOnly = true)
    public List<DepartmentAttendanceReportDTO.InstructorStats> getDepartmentInstructorAttendance(Long departmentId) {

        List<Instructor> departmentInstructors = instructorRepository.findAll().stream()
                .filter(i -> i.getUser().getDepartment() != null &&
                        i.getUser().getDepartment().getId().equals(departmentId))
                .collect(Collectors.toList());

        List<InstructorAttendance> allAttendance = instructorAttendanceRepository
                .findByDepartmentIdAndCurrentSemester(departmentId.intValue());

        Map<Integer, List<InstructorAttendance>> attendanceMap = allAttendance.stream()
                .collect(Collectors.groupingBy(ia -> ia.getInstructor().getId()));

        List<DepartmentAttendanceReportDTO.InstructorStats> report = new ArrayList<>();

        for (Instructor instructor : departmentInstructors) {

            List<InstructorAttendance> records = attendanceMap.getOrDefault(instructor.getId(), Collections.emptyList());

            long totalSessions = records.size();
            long presentCount = records.stream()
                    .filter(r -> "PRESENT".equalsIgnoreCase(r.getStatus()))
                    .count();
            long absentCount = records.stream()
                    .filter(r -> "ABSENT".equalsIgnoreCase(r.getStatus()))
                    .count();

            double rate = totalSessions > 0 ? ((double) presentCount / totalSessions) * 100.0 : 0.0;

            report.add(DepartmentAttendanceReportDTO.InstructorStats.builder()
                    .instructorId(instructor.getId())
                    .instructorName(instructor.getUser().getFirstName() + " " + instructor.getUser().getLastName())
                    .totalSessionsScheduled(totalSessions)
                    .presentCount(presentCount)
                    .absentCount(absentCount)
                    .attendanceRate(Math.round(rate * 10.0) / 10.0)
                    .build());
        }

        return report;
    }

    @Transactional(readOnly = true)
    public List<DepartmentAttendanceReportDTO.StudentStats> getDepartmentStudentAttendance(Long departmentId) {

        List<CourseOffering> offerings = courseOfferingRepository
                .findByCourse_Department_IdAndAcademicSemester_IsCurrentTrue(departmentId);

        List<DepartmentAttendanceReportDTO.StudentStats> report = new ArrayList<>();

        for (CourseOffering offering : offerings) {

            List<Enrollment> enrollments = enrollmentRepository.findByCourseOfferingId(offering.getId());

            for (Enrollment enrollment : enrollments) {
                List<Attendance> records = studentAttendanceRepository
                        .findByStudentAndCourse(enrollment.getStudent().getId(), offering.getId());

                long totalClasses = records.size();

                if (totalClasses == 0)
                    continue;

                long absentClasses = records.stream()
                        .filter(a -> "ABSENT".equalsIgnoreCase(a.getStatus()))
                        .count();

                double absencePercentage = ((double) absentClasses / totalClasses) * 100.0;

                String academicStatus = "ACTIVE";

                if ("NG".equals(enrollment.getFinalGrade()) || absencePercentage >= 25.0) {
                    academicStatus = "NG";
                } else if (absencePercentage >= 20.0) {
                    academicStatus = "WARNING";
                }

                report.add(DepartmentAttendanceReportDTO.StudentStats.builder()
                        .studentId(enrollment.getStudent().getStudentId())
                        .studentName(enrollment.getStudent().getUser().getFirstName() + " " +
                                enrollment.getStudent().getUser().getLastName())
                        .courseCode(offering.getCourse().getCourseCode())
                        .courseTitle(offering.getCourse().getTitle())
                        .sectionName(offering.getSection().getName())
                        .totalSessions(totalClasses)
                        .absentSessions(absentClasses)
                        .absencePercentage(Math.round(absencePercentage * 10.0) / 10.0)
                        .status(academicStatus)
                        .build());
            }
        }

        return report;
    }

}