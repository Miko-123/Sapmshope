package com.hopesapms.app.modules.instructor.repository;

import com.hopesapms.app.modules.instructor.model.Instructor;
import com.hopesapms.app.modules.instructor.model.InstructorAttendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InstructorRepository extends JpaRepository<Instructor, Long> {

    Optional<Instructor> findByUser_Id(Integer userId);

    @Query("SELECT COUNT(i) FROM Instructor i WHERE i.user.department.id = :deptId")
    long countByDepartmentId(@Param("deptId") Long deptId);

    @Query("SELECT i FROM Instructor i JOIN i.user u WHERE " +
            "(LOWER(u.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%'))) " +
            "AND u.isDeleted = false")
    List<Instructor> searchInstructors(@Param("query") String query);
}