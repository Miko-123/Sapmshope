package com.hopesapms.app.controller;

import com.hopesapms.app.dto.ScheduleDTO;
import com.hopesapms.app.service.ScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/schedule")
@RequiredArgsConstructor
@Tag(name = "Schedule (Student)", description = "API for students to view their class schedule (UC-013)")
public class ScheduleController {

    private final ScheduleService scheduleService;

    @GetMapping("/my-schedule")
    @PreAuthorize("hasAuthority('STUDENT')")
    @Operation(summary = "Get my personal class schedule for all enrolled courses")
    public ResponseEntity<List<ScheduleDTO>> getMySchedule(Authentication authentication) {
        List<ScheduleDTO> schedule = scheduleService.getMySchedule(authentication);
        return ResponseEntity.ok(schedule);
    }
}