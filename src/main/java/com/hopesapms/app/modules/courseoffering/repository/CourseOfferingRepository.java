package com.hopesapms.app.modules.courseoffering.repository;

import com.hopesapms.app.modules.courseoffering.model.CourseOffering;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseOfferingRepository extends JpaRepository<CourseOffering, Long> {

       List<CourseOffering> findByAcademicSemester_IdAndCourse_IdAndInstructor_IdAndIsDeletedFalse(
            Long semesterId, 
            Integer courseId, 
            Long instructorId 
    );

        @Query("SELECT co FROM CourseOffering co JOIN FETCH co.course JOIN FETCH co.instructor JOIN FETCH co.section JOIN FETCH co.academicSemester WHERE co.academicSemester.id = :semesterId AND co.isDeleted = false")
        List<CourseOffering> findByAcademicSemesterId(Long semesterId);

        List<CourseOffering> findByAcademicSemester_IdAndIsDeletedFalse(Long semesterId);

        List<CourseOffering> findByAcademicSemester_IdAndCourse_IdAndIsDeletedFalse(Long semesterId, Integer courseId);

        List<CourseOffering> findBySection_IdAndAcademicSemester_IdAndStatusAndIsDeletedFalse(
                        Integer sectionId,
                        Long semesterId,
                        String status);

        List<CourseOffering> findBySectionIdAndAcademicSemesterIdAndStatusIn(
                        Integer sectionId,
                        Long academicSemesterId,
                        List<String> statuses);

        List<CourseOffering> findByInstructorIdAndStatus(Long instructorId, String status);

        List<CourseOffering> findByInstructor_IdAndIsDeletedFalse(Integer instructorId);

        @Query("SELECT co FROM CourseOffering co " +
                        "JOIN FETCH co.course c " +
                        "LEFT JOIN FETCH co.instructor " +
                        "JOIN FETCH co.section " +
                        "JOIN FETCH co.academicSemester " +
                        "WHERE co.academicSemester.id = :semesterId " +
                        "AND c.department.id = :departmentId " +
                        "AND co.isDeleted = false")
        List<CourseOffering> findBySemesterAndDepartment(Long semesterId, Long departmentId);

        @Query("SELECT DISTINCT co FROM CourseOffering co " +
                        "LEFT JOIN FETCH co.scheduleSlots " +
                        "JOIN FETCH co.instructor " +
                        "JOIN FETCH co.section " +
                        "WHERE co.academicSemester.id = :semesterId " +
                        "AND co.status = :status " +
                        "AND co.isDeleted = false")
        List<CourseOffering> findAllActiveInSemester(Long semesterId, String status);

        List<CourseOffering> findByInstructor_User_IdAndIsDeletedFalse(Integer userId);

        boolean existsByAcademicSemester_IdAndCourse_IdAndSection_IdAndIsDeletedFalse(
                        Long semesterId, Integer courseId, Integer sectionId);

        long countByAcademicSemesterIdAndIsDeletedFalse(Long semesterId);

        List<CourseOffering> findByCourse_Department_IdAndAcademicSemester_IsCurrentTrue(Long departmentId);

        @Query("SELECT COUNT(co) FROM CourseOffering co WHERE co.course.department.id = :deptId AND co.status = :status AND co.isDeleted = false")
        long countByDepartmentIdAndStatus(@Param("deptId") Long deptId, @Param("status") String status);
}