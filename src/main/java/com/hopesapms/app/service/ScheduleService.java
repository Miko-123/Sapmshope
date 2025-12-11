package com.hopesapms.app.service;

import com.hopesapms.app.dto.ScheduleResponseDTO;
import com.hopesapms.app.exception.ResourceNotFoundException;
import com.hopesapms.app.model.*;
import com.hopesapms.app.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private static final Logger logger = LoggerFactory.getLogger(ScheduleService.class);

    private final CourseOfferingRepository courseOfferingRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<ScheduleResponseDTO> getInstructorSchedule(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        logger.info("Fetching schedule for User: {} (ID: {})", user.getEmail(), user.getId());

        List<CourseOffering> offerings = courseOfferingRepository
                .findByInstructor_User_IdAndIsDeletedFalse(user.getId());

        logger.info("Found {} course offerings for this instructor", offerings.size());

        List<ScheduleResponseDTO> schedule = new ArrayList<>();

        for (CourseOffering offering : offerings) {
            if (offering.getScheduleSlots() != null) {
                for (CourseSchedule cs : offering.getScheduleSlots()) {
                    logger.info("Processing Slot: Day={}, Periods={}", cs.getDay(), cs.getPeriods());

                    ScheduleResponseDTO dto = new ScheduleResponseDTO();
                    dto.setCourseOfferingId(offering.getId());
                    dto.setCourseName(offering.getCourse().getTitle());
                    dto.setCourseCode(offering.getCourse().getCourseCode());
                    dto.setSectionName(offering.getSection().getName());

                    dto.setDayOfWeek(parseDay(cs.getDay()));

                    TimeSlot timeSlot = parsePeriodsToTime(cs.getPeriods());
                    dto.setStartTime(timeSlot.start);
                    dto.setEndTime(timeSlot.end);

                    dto.setRoom(cs.getRoom());
                    dto.setColor(getColorForId(offering.getId()));

                    schedule.add(dto);
                }
            }
        }
        return schedule;
    }

    private DayOfWeek parseDay(String dayString) {
        if (dayString == null)
            return DayOfWeek.MONDAY;
        try {
            String cleaned = dayString.trim().toUpperCase();
            if (cleaned.startsWith("MON"))
                return DayOfWeek.MONDAY;
            if (cleaned.startsWith("TUE"))
                return DayOfWeek.TUESDAY;
            if (cleaned.startsWith("WED"))
                return DayOfWeek.WEDNESDAY;
            if (cleaned.startsWith("THU"))
                return DayOfWeek.THURSDAY;
            if (cleaned.startsWith("FRI"))
                return DayOfWeek.FRIDAY;
            if (cleaned.startsWith("SAT"))
                return DayOfWeek.SATURDAY;
            if (cleaned.startsWith("SUN"))
                return DayOfWeek.SUNDAY;
            return DayOfWeek.valueOf(cleaned);
        } catch (Exception e) {
            logger.warn("Failed to parse day: {}", dayString);
            return DayOfWeek.MONDAY;
        }
    }

    private TimeSlot parsePeriodsToTime(String periods) {
        int startHour = 8;
        int durationInHours = 1;

        if (periods != null && !periods.isEmpty()) {
            try {
                String[] parts = periods.split("-");
                int firstPeriod = Integer.parseInt(parts[0].trim());
                int lastPeriod = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : firstPeriod;
                startHour = 8 + (firstPeriod - 1);
                durationInHours = (lastPeriod - firstPeriod) + 1;
            } catch (Exception e) {
                logger.warn("Failed to parse periods: {}", periods);
            }
        }

        return new TimeSlot(
                LocalTime.of(startHour, 0),
                LocalTime.of(startHour + durationInHours, 0));
    }

    private String getColorForId(Long id) {
        String[] colors = {
                "bg-blue-100 border-blue-200 text-blue-700",
                "bg-green-100 border-green-200 text-green-700",
                "bg-purple-100 border-purple-200 text-purple-700",
                "bg-orange-100 border-orange-200 text-orange-700",
                "bg-pink-100 border-pink-200 text-pink-700"
        };
        return colors[(int) (id % colors.length)];
    }

    private static class TimeSlot {
        LocalTime start;
        LocalTime end;

        public TimeSlot(LocalTime start, LocalTime end) {
            this.start = start;
            this.end = end;
        }
    }
}