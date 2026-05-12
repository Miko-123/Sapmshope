package com.hopesapms.app.modules.grading.controller;

import com.hopesapms.app.modules.grading.dto.GradingScaleDTO;
import com.hopesapms.app.modules.grading.service.GradingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/grading-scales")
@RequiredArgsConstructor
@Tag(name = "Grading Scale Management", description = "APIs for managing letter grades and score ranges")
public class GradingScaleController {

    private final GradingService gradingService;

    @GetMapping
    @Operation(summary = "Get all grading scales")
    public ResponseEntity<List<GradingScaleDTO>> getAllScales() {
        return ResponseEntity.ok(gradingService.getAllScales());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('REGISTRAR')")
    @Operation(summary = "Update a grading scale range")
    public ResponseEntity<GradingScaleDTO> updateScale(
            @PathVariable Integer id,
            @Valid @RequestBody GradingScaleDTO dto) {

        return ResponseEntity.ok(gradingService.updateScale(id, dto));
    }
}