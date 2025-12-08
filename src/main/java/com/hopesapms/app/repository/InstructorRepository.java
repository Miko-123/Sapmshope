package com.hopesapms.app.repository;

import com.hopesapms.app.model.Course;
import com.hopesapms.app.model.Instructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InstructorRepository extends JpaRepository<Instructor, Integer> {
    Optional<Instructor> findByUserId(Integer userId);

    Optional<Instructor> findByUserUsername(String username);

    @Query("SELECT co.course FROM CourseOffering co WHERE co.instructor = :instructor")
    List<Course> findByInstructor(@Param("instructor") Instructor instructor);

    Optional<Instructor> findByUserUsernameAndUserIsDeletedFalse(String name);



}