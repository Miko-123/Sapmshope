package com.hopesapms.app.service;

import com.hopesapms.app.dto.CreateSectionRequestDTO;
import com.hopesapms.app.dto.SectionResponseDTO;
import com.hopesapms.app.dto.UpdateSectionRequestDTO;
import com.hopesapms.app.exception.ResourceNotFoundException;
import com.hopesapms.app.model.Program;
import com.hopesapms.app.model.Section;
import com.hopesapms.app.model.Student;
import com.hopesapms.app.repository.ProgramRepository;
import com.hopesapms.app.repository.SectionRepository;
import jakarta.persistence.EntityExistsException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SectionService {
    
    private final SectionRepository sectionRepository;
    private final ProgramRepository programRepository; 
    private final AuditLogService auditLogService;

    @Transactional
    public SectionResponseDTO createSection(CreateSectionRequestDTO dto) {
       
        if (sectionRepository.findByNameAndProgramIdAndYearLevelAndIsDeletedFalse(
                dto.getName(), dto.getProgramId(), dto.getYearLevel()).isPresent()) {
            throw new EntityExistsException("Section '" + dto.getName() + "' already exists for this program and year.");
        }

        Program program = programRepository.findById(dto.getProgramId())
                .orElseThrow(() -> new ResourceNotFoundException("Program not found with id: " + dto.getProgramId()));

        Section section = Section.builder()
                .name(dto.getName())
                .yearLevel(dto.getYearLevel())
                .capacity(dto.getCapacity())
                .program(program)
                .isDeleted(false)
                .build();

        Section savedSection = sectionRepository.save(section);
        auditLogService.log("CREATE_SECTION", "Section", savedSection.getId().longValue(), null, savedSection.toString());
        return mapEntityToDto(savedSection);
    }

    @Transactional
    public SectionResponseDTO updateSection(Integer id, UpdateSectionRequestDTO dto) {
        Section section = sectionRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found with id: " + id));

        String oldData = section.toString();

        Optional<Section> existing = sectionRepository.findByNameAndProgramIdAndYearLevelAndIsDeletedFalse(
                dto.getName(), dto.getProgramId(), dto.getYearLevel());
        
        if (existing.isPresent() && !existing.get().getId().equals(id)) {
             throw new EntityExistsException("Section '" + dto.getName() + "' already exists for this program and year.");
        }

        Program program = programRepository.findById(dto.getProgramId())
                .orElseThrow(() -> new ResourceNotFoundException("Program not found with id: " + dto.getProgramId()));

        section.setName(dto.getName());
        section.setYearLevel(dto.getYearLevel());
        section.setCapacity(dto.getCapacity());
        section.setProgram(program);

        Section updatedSection = sectionRepository.save(section);
        auditLogService.log("UPDATE_SECTION", "Section", updatedSection.getId().longValue(), oldData, updatedSection.toString());
        return mapEntityToDto(updatedSection);
    }

     @Transactional
    public void deleteSection(Integer id) {
        Section section = sectionRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found with id: " + id));

        String oldData = section.toString();

        section.setDeleted(true);
        section.setName(section.getName() + "_DELETED_" + section.getId());

        sectionRepository.save(section);
        auditLogService.log("DELETE_SECTION", "Section", section.getId().longValue(), oldData, "DELETED");
    }

    @Transactional(readOnly = true)
    public List<SectionResponseDTO> getAllSections() {
        return sectionRepository.findByIsDeletedFalse().stream()
                .map(this::mapEntityToDto)
                .collect(Collectors.toList());
    }

     @Transactional(readOnly = true)
    public List<Student> getStudentsForSection(Integer sectionId) {
        return sectionRepository.findStudentsBySectionId(sectionId);
    }

    @Transactional(readOnly = true)
    public SectionResponseDTO getSectionById(Integer id) {
        Section section = sectionRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found with id: " + id));
        return mapEntityToDto(section);
    }

    private SectionResponseDTO mapEntityToDto(Section section) {
        SectionResponseDTO dto = new SectionResponseDTO();
        dto.setId(section.getId());
        dto.setName(section.getName());
        dto.setYearLevel(section.getYearLevel());
        dto.setCapacity(section.getCapacity());
        dto.setCreatedAt(section.getCreatedAt());
        dto.setUpdatedAt(section.getUpdatedAt());

        if (section.getProgram() != null) {
            dto.setProgramId(section.getProgram().getId());
            dto.setProgramName(section.getProgram().getName());

            if (section.getProgram().getDepartment() != null) {
                dto.setDepartmentName(section.getProgram().getDepartment().getName());
            }
        }
        return dto;
    }
}
