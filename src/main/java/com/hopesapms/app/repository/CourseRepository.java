package com.hopesapms.app.repository;

import com.hopesapms.app.model.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Integer> {

    Optional<Course> findByIdAndIsDeletedFalse(Integer id);

    Optional<Course> findByCourseCodeAndIsDeletedFalse(String courseCode);


    Page<Course> findByIsDeletedFalse(Pageable pageable);

    Page<Course> findByDepartment_IdAndIsDeletedFalse(Long departmentId, Pageable pageable);

  
    Page<Course> findByAcademicSemester_IdAndIsDeletedFalse(Long semesterId, Pageable pageable);

    @Query("SELECT c FROM Course c JOIN FETCH c.assessments WHERE c.id = :id AND c.isDeleted = false")
    Optional<Course> findByIdWithAssessments(Integer id);

    @Query("SELECT AVG(c.credits) FROM Course c WHERE c.isDeleted = false")
    Double findAverageCredits();
}