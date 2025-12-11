package com.hopesapms.app.service;

import com.hopesapms.app.dto.InstructorAttendanceDTO;
import com.hopesapms.app.dto.MarkInstructorRequest;
import com.hopesapms.app.model.*;
import com.hopesapms.app.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProgramOfficerService {

    private final CourseScheduleRepository courseScheduleRepository;
    private final ClassSessionRepository sessionRepository;
    private final CourseOfferingRepository courseOfferingRepository;

    @Transactional(readOnly = true)
    public List<InstructorAttendanceDTO> getTodaysClasses() {

        String todayStr = LocalDate.now().getDayOfWeek()
                .getDisplayName(TextStyle.SHORT, Locale.ENGLISH); // Returns "Thu" for Thursday

        List<CourseSchedule> todaysSchedules = courseScheduleRepository.findByDay(todayStr);

        List<InstructorAttendanceDTO> result = new ArrayList<>();

        for (CourseSchedule schedule : todaysSchedules) {
            CourseOffering offering = schedule.getCourseOffering();

            if (!"ACTIVE".equals(offering.getStatus()))
                continue;

            InstructorAttendanceDTO dto = new InstructorAttendanceDTO();
            dto.setCourseOfferingId(offering.getId());
            dto.setCourseTitle(offering.getCourse().getTitle());
            dto.setCourseCode(offering.getCourse().getCourseCode());
            dto.setSectionName(offering.getSection().getName());
            dto.setRoomNumber(schedule.getRoom());

            TimeSlot slot = parsePeriods(schedule.getPeriods());
            dto.setStartTime(slot.start);
            dto.setEndTime(slot.end);

            if (offering.getInstructor() != null && offering.getInstructor().getUser() != null) {
                dto.setInstructorName(offering.getInstructor().getUser().getFirstName() + " "
                        + offering.getInstructor().getUser().getLastName());
            } else {
                dto.setInstructorName("TBD");
            }

            Optional<ClassSession> existingSession = sessionRepository.findByCourseOffering_IdAndSessionDate(
                    offering.getId(), LocalDate.now());

            if (existingSession.isPresent()) {
                dto.setSessionId(existingSession.get().getId().longValue());
                dto.setStatus(existingSession.get().getInstructorStatus());
            } else {
                dto.setStatus("PENDING");
            }

            result.add(dto);
        }

        result.sort((a, b) -> a.getStartTime().compareTo(b.getStartTime()));
        return result;
    }

    @Transactional
    public void markInstructorAttendance(MarkInstructorRequest request) {
        LocalDate today = LocalDate.now();
        ClassSession session = sessionRepository.findByCourseOffering_IdAndSessionDate(
                request.getCourseOfferingId(), today).orElse(new ClassSession());

        if (session.getId() == null) {
            CourseOffering offering = courseOfferingRepository.findById(request.getCourseOfferingId())
                    .orElseThrow(() -> new IllegalArgumentException("Course not found"));

            session.setCourseOffering(offering);
            session.setSessionDate(today);
        }

        session.setInstructorStatus(request.getStatus());
        sessionRepository.save(session);
    }

    private TimeSlot parsePeriods(String periods) {
        LocalTime start = LocalTime.of(8, 30);
        LocalTime end = LocalTime.of(10, 30);

        if (periods == null)
            return new TimeSlot(start, end);

        
        if (periods.contains("1") || periods.contains("2")) {
            start = LocalTime.of(8, 30);
            end = LocalTime.of(10, 30);
        } else if (periods.contains("3") || periods.contains("4")) {
            start = LocalTime.of(10, 45);
            end = LocalTime.of(12, 45);
        } else if (periods.contains("5") || periods.contains("6")) {
            start = LocalTime.of(13, 30);
            end = LocalTime.of(15, 30);
        } else if (periods.contains("7") || periods.contains("8")) {
            start = LocalTime.of(15, 45);
            end = LocalTime.of(17, 45);
        }

        return new TimeSlot(start, end);
    }

    private static class TimeSlot {
        LocalTime start;
        LocalTime end;

        TimeSlot(LocalTime s, LocalTime e) {
            this.start = s;
            this.end = e;
        }
    }
}