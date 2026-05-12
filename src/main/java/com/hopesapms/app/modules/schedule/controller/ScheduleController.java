package com.hopesapms.app.modules.schedule.controller;

import com.hopesapms.app.modules.schedule.dto.ScheduleResponseDTO;
import com.hopesapms.app.modules.schedule.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/schedule")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    @GetMapping("/instructor")
    public ResponseEntity<List<ScheduleResponseDTO>> getInstructorSchedule(Authentication authentication) {
        return ResponseEntity.ok(scheduleService.getInstructorSchedule(authentication));
    }
}