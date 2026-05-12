package com.hopesapms.app.modules.program.repository;

import com.hopesapms.app.modules.program.model.Program;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProgramRepository extends JpaRepository<Program, Long> {
    boolean existsByCodeAndIsDeletedFalse(String code);

    boolean existsByNameAndIsDeletedFalse(String name);

    List<Program> findByIsDeletedFalse();

    Optional<Program> findByIdAndIsDeletedFalse(Long id);

    Optional<Program> findByCodeAndIsDeletedFalse(String code);

    Optional<Program> findByNameIgnoreCaseAndDepartmentId(String name, Long departmentId);

    Optional<Program> findByNameAndDepartmentIdAndIsDeletedFalse(String name, Long departmentId);

    @Query("SELECT p FROM Program p WHERE LOWER(p.name) = LOWER(?1) AND p.department.id = ?2 AND p.isDeleted = false")
    Optional<Program> findByNameIgnoreCaseAndDepartmentIdAndIsDeletedFalse(String name, Long departmentId);

    List<Program> findByDepartment_IdAndIsDeletedFalse(Long departmentId);
}
