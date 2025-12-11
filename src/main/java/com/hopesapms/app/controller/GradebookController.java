package com.hopesapms.app.controller;

import com.hopesapms.app.dto.GradebookDTO;
import com.hopesapms.app.dto.SaveScoreRequestDTO;
import com.hopesapms.app.service.GradebookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/gradebook")
@RequiredArgsConstructor
public class GradebookController {

    private final GradebookService gradebookService;

    @GetMapping("/{offeringId}")
    @PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'DEPARTMENT_HEAD')")
    public ResponseEntity<GradebookDTO> getGradebook(@PathVariable Long offeringId) {
        return ResponseEntity.ok(gradebookService.getGradebook(offeringId));
    }

    @PostMapping("/score")
    public ResponseEntity<?> saveScore(@RequestBody SaveScoreRequestDTO dto, Authentication authentication) {
        gradebookService.saveScore(dto, authentication);
        return ResponseEntity.ok().build();
    }
}