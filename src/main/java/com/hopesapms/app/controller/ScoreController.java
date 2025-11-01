package com.hopesapms.app.controller;

import com.hopesapms.app.dto.BulkScoreRequestDTO;
import com.hopesapms.app.dto.BulkUploadResponse;
import com.hopesapms.app.dto.ScoreRequestDTO;
import com.hopesapms.app.dto.ScoreResponseDTO;
import com.hopesapms.app.dto.StudentScoreDTO;
import com.hopesapms.app.service.ScoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/scores")
@RequiredArgsConstructor
@Tag(name = "Score Management (Instructor)", description = "APIs for instructors to manage student scores (UC-008)")
public class ScoreController {

    private final ScoreService scoreService;

    @PostMapping("/single")
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'DEPARTMENT_HEAD', 'INSTRUCTOR')")
    @Operation(summary = "Enter or update a single score for a student")
    public ResponseEntity<ScoreResponseDTO> enterScore(
            @Valid @RequestBody ScoreRequestDTO dto, Authentication authentication) {
        
        ScoreResponseDTO savedScore = scoreService.enterScore(dto, authentication);
        return ResponseEntity.ok(savedScore);
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'DEPARTMENT_HEAD', 'INSTRUCTOR')")
    @Operation(summary = "Enter or update scores for multiple students on one assessment")
    public ResponseEntity<BulkUploadResponse> bulkEnterScores(
            @Valid @RequestBody BulkScoreRequestDTO bulkDto, Authentication authentication) {
        
        BulkUploadResponse response = scoreService.bulkEnterScores(bulkDto, authentication);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my-scores/{courseId}")
    @PreAuthorize("hasAuthority('STUDENT')")
    @Operation(summary = "Get my scores for a specific course (UC-011)")
    public ResponseEntity<List<StudentScoreDTO>> getMyScoresForCourse(
            @PathVariable Integer courseId, Authentication authentication) {
        
        List<StudentScoreDTO> scores = scoreService.getMyScoresForCourse(courseId, authentication);
        return ResponseEntity.ok(scores);
    }
}