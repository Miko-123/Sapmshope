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
    Optional<Course> findByCourseCodeAndIsDeletedFalse(String courseCode);

    @Query("SELECT c FROM Course c WHERE c.isDeleted = false")
    Page<Course> findAllActive(Pageable pageable);

    @Query("SELECT c FROM Course c JOIN FETCH c.department d WHERE d.id = :departmentId AND c.isDeleted = false")
    Page<Course> findByDepartmentId(Integer departmentId, Pageable pageable);

    @Query("SELECT c FROM Course c JOIN FETCH c.academicSemester a WHERE a.id = :semesterId AND c.isDeleted = false")
    Page<Course> findByAcademicSemesterId(Integer semesterId, Pageable pageable);

    @Query("SELECT c FROM Course c JOIN FETCH c.assessments WHERE c.id = :id AND c.isDeleted = false")
    Optional<Course> findByIdWithAssessments(Integer id);

    @Query("SELECT AVG(c.credits) FROM Course c WHERE c.isDeleted = false")
    Double findAverageCredits();
}