package com.hopesapms.app.modules.academicsemester.service;

import com.hopesapms.app.modules.academicsemester.dto.AcademicSemesterRequestDTO;
import com.hopesapms.app.modules.academicsemester.dto.AcademicSemesterResponseDTO;
import com.hopesapms.app.common.exception.ResourceNotFoundException;
import com.hopesapms.app.modules.academicsemester.model.AcademicSemester;
import com.hopesapms.app.modules.academicsemester.repository.AcademicSemesterRepository;
import jakarta.persistence.EntityExistsException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import com.hopesapms.app.modules.auditlog.service.AuditLogService;

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
                .isCurrent(false)
                .status("PLANNED")
                .build();

        AcademicSemester saved = semesterRepository.save(semester);
        auditLogService.log("CREATE_SEMESTER", "AcademicSemester", saved.getId(), null, saved.toString());
        return mapToResponseDTO(saved);
    }

    @Transactional(readOnly = true)
    public void validateSemesterEditable(Long semesterId) {
        AcademicSemester semester = semesterRepository.findById(semesterId)
                .orElseThrow(() -> new ResourceNotFoundException("Semester not found"));

        if ("ARCHIVED".equalsIgnoreCase(semester.getStatus())) {
            throw new IllegalStateException(
                    "Action denied: The semester '" + semester.getName() + "' is ARCHIVED and read-only.");
        }
    }

    @Transactional(readOnly = true)
    public AcademicSemester getCurrentSemester() {
        return semesterRepository.findByIsCurrentTrue()
                .orElseThrow(() -> new ResourceNotFoundException("No active semester defined."));
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

        if ("ARCHIVED".equalsIgnoreCase(semester.getStatus())) {
            throw new IllegalStateException("Cannot update details of an Archived semester.");
        }

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

        if (semester.isCurrent()) {
            throw new IllegalStateException("Cannot delete the currently active semester.");
        }

        String oldData = semester.toString();
        semester.setDeleted(true);
        semesterRepository.save(semester);
        auditLogService.log("DELETE_SEMESTER", "AcademicSemester", id, oldData, "DELETED");
    }

    @Transactional
    public AcademicSemesterResponseDTO setCurrentSemester(Long id) {

        AcademicSemester newCurrent = semesterRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Academic Semester not found"));

        semesterRepository.findByIsDeletedFalse().stream()
                .filter(AcademicSemester::isCurrent)
                .findFirst()
                .ifPresent(oldCurrent -> {

                    if (!oldCurrent.getId().equals(newCurrent.getId())) {
                        oldCurrent.setCurrent(false);
                        oldCurrent.setStatus("ARCHIVED");
                        semesterRepository.save(oldCurrent);
                    }
                });

        newCurrent.setCurrent(true);
        newCurrent.setStatus("ACTIVE");

        AcademicSemester saved = semesterRepository.save(newCurrent);

        auditLogService.log("SET_CURRENT_SEMESTER", "AcademicSemester", saved.getId(), null,
                "Status changed to ACTIVE");
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
        dto.setStatus(entity.getStatus());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }
}