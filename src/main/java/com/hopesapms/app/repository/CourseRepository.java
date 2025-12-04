package com.hopesapms.app.repository;

import com.hopesapms.app.model.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Integer> {

    Optional<Course> findByIdAndIsDeletedFalse(Integer id);

    Optional<Course> findByCourseCodeAndIsDeletedFalse(String courseCode);

    Page<Course> findByDepartment_IdAndIsDeletedFalse(Long departmentId, Pageable pageable);

    boolean existsByIdAndIsDeletedFalse(Integer id);
}