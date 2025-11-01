package com.hopesapms.app.service;

import com.hopesapms.app.dto.AcademicSemesterRequestDTO;
import com.hopesapms.app.dto.AcademicSemesterResponseDTO;
import com.hopesapms.app.exception.ResourceNotFoundException;
import com.hopesapms.app.model.AcademicSemester;
import com.hopesapms.app.repository.AcademicSemesterRepository;
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
    public AcademicSemesterResponseDTO createSemester(AcademicSemesterRequestDTO dto) {
        if (semesterRepository.existsByNameAndIsDeletedFalse(dto.getName())) {
            throw new EntityExistsException("An academic semester with this name already exists.");
        }

        AcademicSemester semester = AcademicSemester.builder()
                .name(dto.getName())
                .year(dto.getYear())
                .type(dto.getType())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .isCurrent(false) // New semesters are never current by default
                .build();

        AcademicSemester saved = semesterRepository.save(semester);
        auditLogService.log("CREATE_SEMESTER", "AcademicSemester", saved.getId(), null, saved.toString());
        return mapToResponseDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<AcademicSemesterResponseDTO> getAllSemesters() {
        return semesterRepository.findByIsDeletedFalse().stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AcademicSemesterResponseDTO getSemesterById(Long id) {
        AcademicSemester semester = semesterRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Academic Semester not found"));
        return mapToResponseDTO(semester);
    }

    @Transactional
    public AcademicSemesterResponseDTO updateSemester(Long id, AcademicSemesterRequestDTO dto) {
        AcademicSemester semester = semesterRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Academic Semester not found"));

        String oldData = semester.toString();
        semester.setName(dto.getName());
        semester.setYear(dto.getYear());
        semester.setType(dto.getType());
        semester.setStartDate(dto.getStartDate());
        semester.setEndDate(dto.getEndDate());

        AcademicSemester updated = semesterRepository.save(semester);
        auditLogService.log("UPDATE_SEMESTER", "AcademicSemester", updated.getId(), oldData, updated.toString());
        return mapToResponseDTO(updated);
    }

    @Transactional
    public void deleteSemester(Long id) {
        AcademicSemester semester = semesterRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Academic Semester not found"));

        String oldData = semester.toString();
        semester.setDeleted(true);
        semesterRepository.save(semester);
        auditLogService.log("DELETE_SEMESTER", "AcademicSemester", id, oldData, "DELETED");
    }

    @Transactional
    public AcademicSemesterResponseDTO setCurrentSemester(Long id) {
        // 1. Find the new semester to make current
        AcademicSemester newCurrent = semesterRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Academic Semester not found"));

        // 2. Find and unset the old current semester (if one exists)
        semesterRepository.findByIsDeletedFalse().stream()
                .filter(AcademicSemester::isCurrent)
                .findFirst()
                .ifPresent(oldCurrent -> {
                    oldCurrent.setCurrent(false);
                    semesterRepository.save(oldCurrent);
                });

        // 3. Set the new semester as current
        newCurrent.setCurrent(true);
        AcademicSemester saved = semesterRepository.save(newCurrent);
        
        auditLogService.log("SET_CURRENT_SEMESTER", "AcademicSemester", saved.getId(), null, saved.toString());
        return mapToResponseDTO(saved);
    }

    private AcademicSemesterResponseDTO mapToResponseDTO(AcademicSemester entity) {
        AcademicSemesterResponseDTO dto = new AcademicSemesterResponseDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setYear(entity.getYear());
        dto.setType(entity.getType());
        dto.setStartDate(entity.getStartDate());
        dto.setEndDate(entity.getEndDate());
        dto.setCurrent(entity.isCurrent());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }
}