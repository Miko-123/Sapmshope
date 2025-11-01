package com.hopesapms.app.repository;

import com.hopesapms.app.model.Program;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ProgramRepository extends JpaRepository<Program, Long> {
    boolean existsByCodeAndIsDeletedFalse(String code);
    boolean existsByNameAndIsDeletedFalse(String name);
    List<Program> findByIsDeletedFalse();
    Optional<Program> findByIdAndIsDeletedFalse(Long id);
    Optional<Program> findByCodeAndIsDeletedFalse(String code);
}
