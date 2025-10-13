package com.hopesapms.app.repository;

import com.hopesapms.app.model.AcademicSemester;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AcademicSemesterRepository extends JpaRepository<AcademicSemester, Integer> {
    @Query("SELECT a FROM AcademicSemester a WHERE a.isDeleted = false")
    Page<AcademicSemester> findAllActive(Pageable pageable);

    @Query("SELECT a FROM AcademicSemester a WHERE a.isCurrent = true AND a.isDeleted = false")
    AcademicSemester findCurrentSemester();

    @Query("SELECT COUNT(a) FROM AcademicSemester a WHERE a.isDeleted = false AND a.endDate < CURRENT_DATE")
    Long countPastSemesters();
}