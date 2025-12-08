package com.hopesapms.app.repository;

import com.hopesapms.app.model.AcademicSemester;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AcademicSemesterRepository extends JpaRepository<AcademicSemester, Long> {

    List<AcademicSemester> findByIsDeletedFalse();

    Optional<AcademicSemester> findByIdAndIsDeletedFalse(Long id);

    boolean existsByNameAndIsDeletedFalse(String name);

    Optional<AcademicSemester> findByIsCurrentTrue();
    
    Optional<AcademicSemester> findByNameAndIsDeletedFalse(String name);
}