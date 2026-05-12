package com.hopesapms.app.modules.course.repository;

import com.hopesapms.app.modules.course.model.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Integer> {

    Optional<Course> findByIdAndIsDeletedFalse(Integer id);

    Optional<Course> findByCourseCodeAndIsDeletedFalse(String courseCode);

    Page<Course> findByDepartment_IdAndIsDeletedFalse(Long departmentId, Pageable pageable);

    @Query("SELECT c FROM Course c WHERE " +
            "(LOWER(c.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(c.courseCode) LIKE LOWER(CONCAT('%', :query, '%'))) " +
            "AND c.isDeleted = false")
    List<Course> searchCourses(@Param("query") String query);

    @Query("SELECT c FROM Course c WHERE " +
            "(LOWER(c.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(c.courseCode) LIKE LOWER(CONCAT('%', :query, '%'))) " +
            "AND c.department.id = :deptId AND c.isDeleted = false")
    List<Course> searchCoursesByDepartment(@Param("query") String query, @Param("deptId") Long deptId);

    boolean existsByIdAndIsDeletedFalse(Integer id);
}