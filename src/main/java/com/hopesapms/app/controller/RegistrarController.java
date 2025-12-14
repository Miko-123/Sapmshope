package com.hopesapms.app.controller;

import com.hopesapms.app.dto.RegistrarDashboardDTO;
import com.hopesapms.app.service.RegistrarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/registrar")
@RequiredArgsConstructor
@Tag(name = "Registrar Dashboard", description = "Operations specific to the Registrar role")
public class RegistrarController {

    private final RegistrarService registrarService;

    @GetMapping("/stats")
    @PreAuthorize("hasAnyAuthority('REGISTRAR', 'SYSTEM_ADMIN')")
    @Operation(summary = "Get dashboard statistics for the current active semester")
    public ResponseEntity<RegistrarDashboardDTO> getDashboardStats() {
        return ResponseEntity.ok(registrarService.getDashboardStats());
    }
}