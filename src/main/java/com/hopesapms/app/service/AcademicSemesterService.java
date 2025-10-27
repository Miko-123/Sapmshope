package com.hopesapms.app.service;

import com.hopesapms.app.dto.AcademicSemesterDTO;
import com.hopesapms.app.model.AcademicSemester;
import com.hopesapms.app.repository.AcademicSemesterRepository;
import com.hopesapms.app.exception.ResourceNotFoundException;
import jakarta.persistence.EntityExistsException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AcademicSemesterService {

    private final AcademicSemesterRepository semesterRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public AcademicSemesterDTO createSemester(AcademicSemesterDTO dto) {
        // Business Logic: Check for uniqueness
        if (semesterRepository.existsByNameAndIsDeletedFalse(dto.getName())) {
            throw new EntityExistsException("A semester with name '" + dto.getName() + "' already exists.");
        }
        
        // Business Logic: Ensure end date is after start date
        if (dto.getStartDate().isAfter(dto.getEndDate())) {
            throw new IllegalArgumentException("Semester end date must be after the start date.");
        }

        AcademicSemester semester = new AcademicSemester();
        mapDtoToEntity(dto, semester);

        AcademicSemester savedSemester = semesterRepository.save(semester);
        
        // Postcondition: Log the action (savedSemester.getId() is now a Long)
        auditLogService.log("CREATE_SEMESTER", "AcademicSemester", savedSemester.getId(), null, savedSemester.toString());
        
        return mapEntityToDto(savedSemester);
    }

    @Transactional(readOnly = true)
    public List<AcademicSemesterDTO> getAllSemesters() {
        return semesterRepository.findByIsDeletedFalse()
                .stream()
                .map(this::mapEntityToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AcademicSemesterDTO getSemesterById(Long id) {
        AcademicSemester semester = semesterRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("AcademicSemester not found with id: " + id));
        return mapEntityToDto(semester);
    }

    @Transactional
    public AcademicSemesterDTO updateSemester(Long id, AcademicSemesterDTO dto) {
        AcademicSemester semester = semesterRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("AcademicSemester not found with id: " + id));

        // Business Logic: Ensure end date is after start date
        if (dto.getStartDate().isAfter(dto.getEndDate())) {
            throw new IllegalArgumentException("Semester end date must be after the start date.");
        }
        
        String oldData = semester.toString();
        mapDtoToEntity(dto, semester);
        
        AcademicSemester updatedSemester = semesterRepository.save(semester);
        
        // Postcondition: Log the update
        auditLogService.log("UPDATE_SEMESTER", "AcademicSemester", updatedSemester.getId(), oldData, updatedSemester.toString());
        
        return mapEntityToDto(updatedSemester);
    }

    @Transactional
    public void deleteSemester(Long id) {
        AcademicSemester semester = semesterRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("AcademicSemester not found with id: " + id));
        
        String oldData = semester.toString();
        
        // Soft delete
        semester.setDeleted(true);
        semesterRepository.save(semester);
        
        // Postcondition: Log the deletion
        auditLogService.log("DELETE_SEMESTER", "AcademicSemester", semester.getId(), oldData, "DELETED");
    }

    // --- Helper Methods ---

    private AcademicSemesterDTO mapEntityToDto(AcademicSemester entity) {
        AcademicSemesterDTO dto = new AcademicSemesterDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setYear(entity.getYear());
        dto.setType(entity.getType());
        dto.setStartDate(entity.getStartDate());
        dto.setEndDate(entity.getEndDate());
        dto.setIsCurrent(entity.isCurrent()); // Use isCurrent() for boolean
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    private void mapDtoToEntity(AcademicSemesterDTO dto, AcademicSemester entity) {
        entity.setName(dto.getName());
        entity.setYear(dto.getYear());
        entity.setType(dto.getType());
        entity.setStartDate(dto.getStartDate());
        entity.setEndDate(dto.getEndDate());
        entity.setCurrent(dto.getIsCurrent() != null ? dto.getIsCurrent() : false);
    }
}