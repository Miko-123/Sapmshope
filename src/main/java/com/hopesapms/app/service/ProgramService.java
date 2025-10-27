package com.hopesapms.app.service;

import com.hopesapms.app.dto.ProgramDTO;
import com.hopesapms.app.exception.ResourceNotFoundException;
import com.hopesapms.app.model.Department;
import com.hopesapms.app.model.Program;
import com.hopesapms.app.repository.DepartmentRepository;
import com.hopesapms.app.repository.ProgramRepository;
import jakarta.persistence.EntityExistsException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProgramService {

    private final ProgramRepository programRepository;
    private final DepartmentRepository departmentRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public ProgramDTO createProgram(ProgramDTO dto) {
        if (programRepository.existsByCodeAndIsDeletedFalse(dto.getCode())) {
            throw new EntityExistsException("Program code already exists: " + dto.getCode());
        }
        if (programRepository.existsByNameAndIsDeletedFalse(dto.getName())) {
            throw new EntityExistsException("Program name already exists: " + dto.getName());
        }

        Department department = departmentRepository.findByIdAndIsDeletedFalse(dto.getDepartmentId().longValue())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));

        Program program = Program.builder()
                .name(dto.getName())
                .code(dto.getCode())
                .department(department)
                .totalCreditRequired(dto.getTotalCreditRequired())
                .description(dto.getDescription())
                .build();

        Program saved = programRepository.save(program);
        auditLogService.log("CREATE_PROGRAM", "Program", saved.getId(), null, saved.toString());

        return mapToDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<ProgramDTO> getAllPrograms() {
        return programRepository.findByIsDeletedFalse()
                .stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProgramDTO getProgramById(Long id) {
        Program program = programRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Program not found with id: " + id));
        return mapToDTO(program);
    }

    @Transactional
    public ProgramDTO updateProgram(Long id, ProgramDTO dto) {
        Program program = programRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Program not found with id: " + id));

        String oldData = program.toString();

        Department department = departmentRepository.findByIdAndIsDeletedFalse(dto.getDepartmentId().longValue())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));

        program.setName(dto.getName());
        program.setCode(dto.getCode());
        program.setDepartment(department);
        program.setTotalCreditRequired(dto.getTotalCreditRequired());
        program.setDescription(dto.getDescription());

        Program updated = programRepository.save(program);
        auditLogService.log("UPDATE_PROGRAM", "Program", updated.getId(), oldData, updated.toString());

        return mapToDTO(updated);
    }

    @Transactional
    public void deleteProgram(Long id) {
        Program program = programRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Program not found with id: " + id));

        String oldData = program.toString();
        program.setDeleted(true);
        programRepository.save(program);

        auditLogService.log("DELETE_PROGRAM", "Program", program.getId(), oldData, "DELETED");
    }

    private ProgramDTO mapToDTO(Program entity) {
        ProgramDTO dto = new ProgramDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setCode(entity.getCode());
        dto.setDepartmentId(entity.getDepartment().getId());
        dto.setTotalCreditRequired(entity.getTotalCreditRequired());
        dto.setDescription(entity.getDescription());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
