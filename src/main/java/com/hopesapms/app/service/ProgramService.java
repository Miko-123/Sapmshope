package com.hopesapms.app.service;

import com.hopesapms.app.dto.ProgramRequestDTO;
import com.hopesapms.app.dto.ProgramResponseDTO;
import com.hopesapms.app.exception.ResourceNotFoundException;
import com.hopesapms.app.model.Department;
import com.hopesapms.app.model.Program;
import com.hopesapms.app.model.User;
import com.hopesapms.app.repository.DepartmentRepository;
import com.hopesapms.app.repository.ProgramRepository;
import com.hopesapms.app.repository.UserRepository; // Import this
import jakarta.persistence.EntityExistsException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException; // Import this
import org.springframework.security.core.Authentication; // Import this
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
    private final UserRepository userRepository;

    @Transactional
    public ProgramResponseDTO createProgram(ProgramRequestDTO dto, Authentication authentication) {

        checkUserAuthorityForDepartment(authentication, dto.getDepartmentId(), "create program in");

        if (programRepository.existsByCodeAndIsDeletedFalse(dto.getCode())) {
            throw new EntityExistsException("Program code already exists: " + dto.getCode());
        }
        if (programRepository.existsByNameAndIsDeletedFalse(dto.getName())) {
            throw new EntityExistsException("Program name already exists: " + dto.getName());
        }

        Department department = departmentRepository.findByIdAndIsDeletedFalse(dto.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));

        // --- 3. Create, Save, and Log ---
        Program program = Program.builder()
                .name(dto.getName())
                .code(dto.getCode())
                .department(department)
                .totalCreditRequired(dto.getTotalCreditRequired())
                .description(dto.getDescription())
                .build();

        Program saved = programRepository.save(program);
        auditLogService.log("CREATE_PROGRAM", "Program", saved.getId(), null, saved.toString());

        return mapToResponseDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<ProgramResponseDTO> getProgramsByDepartmentId(Long departmentId) {

        return programRepository.findByDepartment_IdAndIsDeletedFalse(departmentId)
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());

    }

    @Transactional(readOnly = true)
    public List<ProgramResponseDTO> getAllPrograms() {
        return programRepository.findByIsDeletedFalse()
                .stream().map(this::mapToResponseDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProgramResponseDTO getProgramById(Long id) {
        Program program = programRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Program not found with id: " + id));
        return mapToResponseDTO(program);
    }

    @Transactional
    public ProgramResponseDTO updateProgram(Long id, ProgramRequestDTO dto, Authentication authentication) {
        Program program = programRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Program not found with id: " + id));

        checkUserAuthorityForDepartment(authentication, program.getDepartment().getId(), "update");

        if (!program.getDepartment().getId().equals(dto.getDepartmentId())) {
            checkUserAuthorityForDepartment(authentication, dto.getDepartmentId(), "move program to");
        }

        String oldData = program.toString();

        Department department = departmentRepository.findByIdAndIsDeletedFalse(dto.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));

        program.setName(dto.getName());
        program.setCode(dto.getCode());
        program.setDepartment(department);
        program.setTotalCreditRequired(dto.getTotalCreditRequired());
        program.setDescription(dto.getDescription());

        Program updated = programRepository.save(program);
        auditLogService.log("UPDATE_PROGRAM", "Program", updated.getId(), oldData, updated.toString());

        return mapToResponseDTO(updated);
    }

    @Transactional
    public void deleteProgram(Long id, Authentication authentication) {
        Program program = programRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Program not found with id: " + id));

        checkUserAuthorityForDepartment(authentication, program.getDepartment().getId(), "delete");

        String oldData = program.toString();
        program.setDeleted(true);
        programRepository.save(program);

        auditLogService.log("DELETE_PROGRAM", "Program", program.getId(), oldData, "DELETED");
    }

    private User getUserFromAuth(Authentication authentication) {
        return userRepository.findByEmailAndIsDeletedFalse(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Logged-in user not found."));
    }

    private void checkUserAuthorityForDepartment(Authentication authentication, Long departmentId, String action) {
        User user = getUserFromAuth(authentication);

        boolean isSystemAdmin = user.getRoles().stream()
                .anyMatch(role -> "SYSTEM_ADMIN".equals(role.getName()));
        if (isSystemAdmin)
            return;

        boolean isDeptHead = user.getRoles().stream()
                .anyMatch(role -> "DEPARTMENT_HEAD".equals(role.getName()));

        if (isDeptHead && user.getDepartment() != null && user.getDepartment().getId().equals(departmentId)) {
            return;
        }

        throw new AccessDeniedException("You do not have permission to " + action + " this department.");
    }

    private ProgramResponseDTO mapToResponseDTO(Program entity) {
        ProgramResponseDTO dto = new ProgramResponseDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setCode(entity.getCode());
        dto.setTotalCreditRequired(entity.getTotalCreditRequired());
        dto.setDescription(entity.getDescription());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());

        if (entity.getDepartment() != null) {
            dto.setDepartmentId(entity.getDepartment().getId());
            dto.setDepartmentName(entity.getDepartment().getName());
        }

        return dto;
    }
}