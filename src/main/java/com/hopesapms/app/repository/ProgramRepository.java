package com.hopesapms.app.repository;

import com.hopesapms.app.model.Program;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProgramRepository extends JpaRepository<Program, Integer> {
    Optional<Program> findByCodeAndIsDeletedFalse(String code);

    @Query("SELECT p FROM Program p WHERE p.isDeleted = false")
    Page<Program> findAllActive(Pageable pageable);

    @Query("SELECT p FROM Program p JOIN FETCH p.department d WHERE d.id = :departmentId AND p.isDeleted = false")
    Page<Program> findByDepartmentId(Integer departmentId, Pageable pageable);

    @Query("SELECT AVG(p.totalCreditRequired) FROM Program p WHERE p.isDeleted = false")
    Double findAverageCreditRequired();
}