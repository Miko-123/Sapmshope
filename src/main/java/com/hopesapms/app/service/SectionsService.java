package com.hopesapms.app.service;

import com.hopesapms.app.dto.SectionsDTO;
import com.hopesapms.app.model.Section;
import com.hopesapms.app.repository.SectionsRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SectionsService {

    private final SectionsRepository sectionsRepository;

    public SectionsService(SectionsRepository sectionsRepository) {
        this.sectionsRepository = sectionsRepository;
    }

    public List<SectionsDTO> getAllSections() {
        return sectionsRepository.findByIsDeletedFalse()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private SectionsDTO convertToDTO(Section section) {
    return new SectionsDTO(
            section.getId(),
            section.getName(),
            section.getYearLevel(),
            section.getProgram().getId(),
            section.getProgram().getName(),
            section.getCreatedAt(),
            section.getUpdatedAt()
    );
}

}
