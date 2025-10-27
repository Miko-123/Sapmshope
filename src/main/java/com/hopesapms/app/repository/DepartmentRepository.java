package com.hopesapms.app.repository;

import com.hopesapms.app.model.Department;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {

    Optional<Department> findByIdAndIsDeletedFalse(Long id);

    Optional<Department> findByCodeAndIsDeletedFalse(String code);
    
    Page<Department> findByIsDeletedFalse(Pageable pageable);

    List<Department> findByIsDeletedFalse();

    long countByIsDeletedFalse();

    boolean existsByCodeAndIsDeletedFalse(String code);

    boolean existsByNameAndIsDeletedFalse(String name);
}