package com.hopesapms.app.controller;

import com.hopesapms.app.dto.SectionsDTO;
import com.hopesapms.app.service.SectionsService;


import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/sections")
public class SectionsController {

    private final SectionsService sectionsService;

    public SectionsController(SectionsService sectionsService) {
        this.sectionsService = sectionsService;
    }

    @GetMapping("/all")
    @PreAuthorize("hasAuthority('INSTRUCTOR')")
    public List<SectionsDTO> getAllSections() {
        return sectionsService.getAllSections();
    }
}
