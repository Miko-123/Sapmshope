package com.hopesapms.app.repository;

import com.hopesapms.app.model.CourseOffering;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseOfferingRepository extends JpaRepository<CourseOffering, Long> {

    @Query("SELECT co FROM CourseOffering co JOIN FETCH co.course JOIN FETCH co.instructor JOIN FETCH co.section JOIN FETCH co.academicSemester WHERE co.academicSemester.id = :semesterId AND co.isDeleted = false")
    List<CourseOffering> findByAcademicSemesterId(Long semesterId);

    List<CourseOffering> findByAcademicSemester_IdAndIsDeletedFalse(Long semesterId);
}