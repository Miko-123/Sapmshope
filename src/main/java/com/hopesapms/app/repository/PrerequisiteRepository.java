package com.hopesapms.app.repository;

import com.hopesapms.app.model.Prerequisite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PrerequisiteRepository extends JpaRepository<Prerequisite, Integer> {
    @Query("SELECT p FROM Prerequisite p WHERE p.isDeleted = false")
    Page<Prerequisite> findAllActive(Pageable pageable);

    @Query("SELECT p FROM Prerequisite p JOIN FETCH p.course JOIN FETCH p.prerequisiteCourse WHERE p.course.id = :courseId AND p.isDeleted = false")
    List<Prerequisite> findByCourseId(Integer courseId);
}