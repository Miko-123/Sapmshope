package com.hopesapms.app.repository;

import com.hopesapms.app.model.Department;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Integer> {
    Optional<Department> findByCodeAndIsDeletedFalse(String code);

    @Query("SELECT d FROM Department d WHERE d.isDeleted = false")
    Page<Department> findAllActive(Pageable pageable);

    @Query("SELECT COUNT(d) FROM Department d WHERE d.isDeleted = false")
    Long countActiveDepartments();
}