package com.hopesapms.app.repository;

import com.hopesapms.app.model.CourseObjective;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseObjectiveRepository extends JpaRepository<CourseObjective, Integer> {
    @Query("SELECT co FROM CourseObjective co JOIN FETCH co.course c WHERE c.id = :courseId")
    List<CourseObjective> findByCourseIdWithCourse(Integer courseId);
}