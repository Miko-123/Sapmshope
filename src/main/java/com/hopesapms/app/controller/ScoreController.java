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

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/scores")
@RequiredArgsConstructor
@Tag(name = "Score Management", description = "APIs for instructors to enter grades and students to view them")
public class ScoreController {

    private final ScoreService scoreService;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'DEPARTMENT_HEAD', 'SYSTEM_ADMIN')")
    @Operation(summary = "Enter or update a single score for a student")
    public ResponseEntity<List<ScoreResponseDTO>> enterScore(
            @Valid @RequestBody List<ScoreRequestDTO> dtoList,
            Authentication authentication) {

        List<ScoreResponseDTO> savedScores = scoreService.enterScore(dtoList, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedScores);
    }
    

    @PostMapping("/bulk")
    @PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'DEPARTMENT_HEAD', 'SYSTEM_ADMIN')")
    @Operation(summary = "Bulk upload scores for a single assessment")
    public ResponseEntity<BulkUploadResponse> bulkEnterScores(
            @Valid @RequestBody BulkScoreRequestDTO bulkDto,
            Authentication authentication) {

        BulkUploadResponse response = scoreService.bulkEnterScores(bulkDto, authentication);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my-scores/{courseId}")
    @PreAuthorize("hasAuthority('STUDENT')")
    @Operation(summary = "Get all my scores for a specific course")
    public ResponseEntity<List<StudentScoreDTO>> getMyScoresForCourse(
            @PathVariable Integer courseId,
            Authentication authentication) {

        List<StudentScoreDTO> scores = scoreService.getMyScoresForCourse(courseId, authentication);
        return ResponseEntity.ok(scores);
    }

    @GetMapping("/my-student-scores/{sectionId}/{courseTitle}")
    @PreAuthorize("hasAuthority('INSTRUCTOR')")
    public ResponseEntity<Page<ScoreResponseDTO>> getMyStudentScores(
            @PathVariable Integer sectionId,
            @PathVariable String courseTitle,
            @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(
                scoreService.getSectionScores(sectionId, courseTitle, pageable));
    }

}