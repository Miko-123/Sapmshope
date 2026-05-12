package com.hopesapms.app.modules.student.repository;

import com.hopesapms.app.modules.analytics.dto.CohortDTO;
import com.hopesapms.app.modules.department.dto.DepartmentRetentionDTO;
import com.hopesapms.app.modules.department.dto.DepartmentStatusDTO;
import com.hopesapms.app.modules.academicsemester.dto.StatusSummaryDTO;
import com.hopesapms.app.modules.section.model.Section;
import com.hopesapms.app.modules.student.model.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Integer> {

        long countByIsDeletedFalse();

        boolean existsByStudentIdAndIsDeletedFalse(String studentId);

        List<Student> findBySection_IdAndIsDeletedFalse(Integer sectionId);

        List<Student> findBySectionIdAndIsDeletedFalse(Integer sectionId);

        @EntityGraph(attributePaths = { "user", "program", "department" })
        Optional<Student> findByUserId(Integer userId);

        Optional<Student> findByStudentId(String studentId);

        @EntityGraph(attributePaths = { "user", "program", "department" })
        Optional<Student> findByStudentIdAndIsDeletedFalse(String studentId);

        @EntityGraph(attributePaths = { "user", "program", "department" })
        Page<Student> findAllByIsDeletedFalse(Pageable pageable);

        @EntityGraph(attributePaths = { "user", "program", "department" })
        Page<Student> findByProgramIdAndIsDeletedFalse(Integer programId, Pageable pageable);

        @EntityGraph(attributePaths = { "user", "program", "department" })
        Page<Student> findByDepartmentIdAndIsDeletedFalse(Integer departmentId, Pageable pageable);

        @EntityGraph(attributePaths = { "user", "program", "department" })
        Page<Student> findByStatusAndIsDeletedFalse(String status, Pageable pageable);

        @EntityGraph(attributePaths = { "user", "program", "department" })
        Page<Student> findByEnrollmentDateBetweenAndIsDeletedFalse(LocalDate startDate, LocalDate endDate,
                        Pageable pageable);

        @EntityGraph(attributePaths = { "user", "program", "department" })
        Page<Student> findByYearLevelAndProgramIdAndIsDeletedFalse(Integer yearLevel, Integer programId,
                        Pageable pageable);

        @EntityGraph(attributePaths = { "user", "program", "department" })
        List<Student> findByStudentIdInAndIsDeletedFalse(List<String> studentIds);

        List<Student> findByProgram_IdAndYearLevelAndIsDeletedFalse(Integer programId, Integer yearLevel);

        @Query("SELECT s.program.id AS programId, s.status AS status, COUNT(s) AS count " +
                        "FROM Student s WHERE s.isDeleted = false " +
                        "GROUP BY s.program.id, s.status")
        List<Object[]> countStudentsByProgramAndStatus();

        @Query("SELECT s FROM Student s JOIN s.user u WHERE u.password IS NULL AND s.isDeleted = false AND u.isDeleted = false")
        @EntityGraph(attributePaths = { "user", "program", "department" })
        Page<Student> findStudentsWithIncompleteProfiles(Pageable pageable);

        @Query("SELECT s FROM Student s JOIN s.user u WHERE " +
                        "(LOWER(u.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
                        "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
                        "LOWER(s.studentId) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
                        "LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
                        "s.isDeleted = false")
        Page<Student> searchStudents(String query, Pageable pageable);

        Optional<Student> findByUser_Id(Integer id);

        @Query("SELECT COUNT(s) FROM Student s WHERE s.program.department.id = :deptId AND s.isDeleted = false")
        long countByDepartmentId(@Param("deptId") Long deptId);

        @Query("SELECT COUNT(s) FROM Student s WHERE s.program.department.id = :deptId AND s.yearLevel = :yearLevel AND s.isDeleted = false")
        long countByDepartmentIdAndYearLevel(@Param("deptId") Long deptId, @Param("yearLevel") int yearLevel);

        @Query("SELECT s FROM Student s JOIN s.user u WHERE " +
                        "(LOWER(u.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
                        "LOWER(s.studentId) LIKE LOWER(CONCAT('%', :query, '%'))) " +
                        "AND s.department.id = :deptId AND s.isDeleted = false")
        Page<Student> searchStudentsByDepartment(@Param("query") String query, @Param("deptId") Long deptId,
                        Pageable pageable);

        @Query("SELECT s FROM Section s WHERE LOWER(s.name) = LOWER(?1) AND s.program.id = ?2 AND s.yearLevel = ?3 AND s.isDeleted = false")
    Optional<Section> findByNameIgnoreCaseAndProgramIdAndYearLevelAndIsDeletedFalse(String name, Long programId, Integer yearLevel);

     @Query(nativeQuery = true, value = """
        SELECT
          SUM(CASE WHEN completion_rate >= 85 AND gpa >= :minGpa AND failed_course = 0 THEN 1 ELSE 0 END) as onTrack,
          SUM(CASE WHEN completion_rate >= 70 AND (completion_rate < 85 OR gpa < :minGpa OR failed_course > 0) THEN 1 ELSE 0 END) as atRisk,
          SUM(CASE WHEN completion_rate < 70 OR failed_course > 0 THEN 1 ELSE 0 END) as behindSchedule,
          COUNT(*) as total
        FROM (
          SELECT s.id,
                 COALESCE(SUM(CASE WHEN e.final_grade != 'F' THEN c.credits ELSE 0 END),0) / COALESCE(SUM(c.credits),1)*100 AS completion_rate,
                 COALESCE(SUM(c.credits * CASE
                        WHEN e.final_grade = 'A' THEN 4.0
                        WHEN e.final_grade = 'A-' THEN 3.7
                        WHEN e.final_grade = 'B+' THEN 3.3
                        WHEN e.final_grade = 'B' THEN 3.0
                        WHEN e.final_grade = 'B-' THEN 2.7
                        WHEN e.final_grade = 'C+' THEN 2.3
                        WHEN e.final_grade = 'C' THEN 2.0
                        WHEN e.final_grade = 'C-' THEN 1.7
                        WHEN e.final_grade = 'D+' THEN 1.3
                        WHEN e.final_grade = 'D' THEN 1.0
                        ELSE 0.0 END
                 ) / COALESCE(SUM(c.credits),1),0) AS gpa,
                 SUM(CASE WHEN e.final_grade = 'F' THEN 1 ELSE 0 END) as failed_course
          FROM students s
          LEFT JOIN enrollments e ON e.student_id = s.id
          LEFT JOIN course_offering co ON co.id = e.course_offering_id
          LEFT JOIN course c ON c.id = co.course_id
          LEFT JOIN academic_semester sem ON sem.id = co.academic_semester_id
          WHERE (:semesterId IS NULL OR sem.id = :semesterId)
          GROUP BY s.id
        ) t
    """)
    StatusSummaryDTO getStatusSummary(@Param("semesterId") Long semesterId, @Param("minGpa") double minGpa);

    @Query(nativeQuery = true, value = """
        SELECT d.name as departmentName,
               SUM(CASE WHEN t.completion_rate >= 85 AND t.gpa >= :minGpa AND t.failed_course = 0 THEN 1 ELSE 0 END) as onTrack,
               SUM(CASE WHEN t.completion_rate >= 70 AND (t.completion_rate < 85 OR t.gpa < :minGpa OR t.failed_course > 0) THEN 1 ELSE 0 END) as atRisk,
               SUM(CASE WHEN t.completion_rate < 70 OR t.failed_course > 0 THEN 1 ELSE 0 END) as behind
        FROM students s
        LEFT JOIN departments d ON d.id = s.department_id
        LEFT JOIN (
            SELECT s.id,
                   COALESCE(SUM(CASE WHEN e.final_grade != 'F' THEN c.credits ELSE 0 END),0) / COALESCE(SUM(c.credits),1)*100 AS completion_rate,
                   COALESCE(SUM(c.credits * CASE
                          WHEN e.final_grade = 'A' THEN 4.0
                          WHEN e.final_grade = 'A-' THEN 3.7
                          WHEN e.final_grade = 'B+' THEN 3.3
                          WHEN e.final_grade = 'B' THEN 3.0
                          WHEN e.final_grade = 'B-' THEN 2.7
                          WHEN e.final_grade = 'C+' THEN 2.3
                          WHEN e.final_grade = 'C' THEN 2.0
                          WHEN e.final_grade = 'C-' THEN 1.7
                          WHEN e.final_grade = 'D+' THEN 1.3
                          WHEN e.final_grade = 'D' THEN 1.0
                          ELSE 0.0 END
                   ) / COALESCE(SUM(c.credits),1),0) AS gpa,
                   SUM(CASE WHEN e.final_grade = 'F' THEN 1 ELSE 0 END) as failed_course
            FROM students s
            LEFT JOIN enrollments e ON e.student_id = s.id
            LEFT JOIN course_offering co ON co.id = e.course_offering_id
            LEFT JOIN course c ON c.id = co.course_id
            LEFT JOIN academic_semester sem ON sem.id = co.academic_semester_id
            WHERE (:semesterId IS NULL OR sem.id = :semesterId)
            GROUP BY s.id
        ) t ON t.id = s.id
        GROUP BY d.name
    """)
    List<DepartmentStatusDTO> getDepartmentStatus(@Param("semesterId") Long semesterId, @Param("minGpa") double minGpa);

    @Query(nativeQuery = true, value = """
        SELECT s.student_id as id,
               p.name as name,
               COUNT(DISTINCT s.id) as total,
               SUM(CASE WHEN t.completion_rate >= 85 AND t.gpa >= :minGpa AND t.failed_course = 0 THEN 1 ELSE 0 END) as onTrack,
               AVG(t.completion_rate) as completionRate,
               AVG(t.gpa) as avgGpa
        FROM students s
        LEFT JOIN program p ON p.id = s.program_id
        LEFT JOIN (
            SELECT s.id,
                   COALESCE(SUM(CASE WHEN e.final_grade != 'F' THEN c.credits ELSE 0 END),0) / COALESCE(SUM(c.credits),1)*100 AS completion_rate,
                   COALESCE(SUM(c.credits * CASE
                          WHEN e.final_grade = 'A' THEN 4.0
                          WHEN e.final_grade = 'A-' THEN 3.7
                          WHEN e.final_grade = 'B+' THEN 3.3
                          WHEN e.final_grade = 'B' THEN 3.0
                          WHEN e.final_grade = 'B-' THEN 2.7
                          WHEN e.final_grade = 'C+' THEN 2.3
                          WHEN e.final_grade = 'C' THEN 2.0
                          WHEN e.final_grade = 'C-' THEN 1.7
                          WHEN e.final_grade = 'D+' THEN 1.3
                          WHEN e.final_grade = 'D' THEN 1.0
                          ELSE 0.0 END
                   ) / COALESCE(SUM(c.credits),1),0) AS gpa,
                   SUM(CASE WHEN e.final_grade = 'F' THEN 1 ELSE 0 END) as failed_course
            FROM students s
            LEFT JOIN enrollments e ON e.student_id = s.id
            LEFT JOIN course_offering co ON co.id = e.course_offering_id
            LEFT JOIN course c ON c.id = co.course_id
            LEFT JOIN academic_semester sem ON sem.id = co.academic_semester_id
            WHERE (:semesterId IS NULL OR sem.id = :semesterId)
            GROUP BY s.id
        ) t ON t.id = s.id
        GROUP BY p.id, p.name, s.student_id
    """)
    List<CohortDTO> getCohorts(@Param("semesterId") Long semesterId, @Param("minGpa") double minGpa);
    
    @Query("SELECT COUNT(s) FROM Student s WHERE s.isDeleted = false AND s.status = 'ACTIVE'")
    long countActiveStudents();

    @Query("SELECT COUNT(s) FROM Student s WHERE s.isDeleted = false")
    long countTotalStudents();

    @Query("SELECT COUNT(s) FROM Student s WHERE s.isDeleted = false AND s.status IN ('WITHDRAWN', 'DISMISSED')")
    long countAttritionStudents();

    // Group by Enrollment Year for Trend Chart
    @Query("SELECT YEAR(s.enrollmentDate) as year, " +
           "COUNT(s) as total, " +
           "SUM(CASE WHEN s.status = 'ACTIVE' OR s.status = 'GRADUATED' THEN 1 ELSE 0 END) as retained " +
           "FROM Student s WHERE s.isDeleted = false " +
           "GROUP BY YEAR(s.enrollmentDate) ORDER BY YEAR(s.enrollmentDate) ASC")
    List<Object[]> findRetentionByYearCohort();

    // Group by Year Level (Class Standing)
    @Query("SELECT s.yearLevel, COUNT(s) FROM Student s " +
           "WHERE s.isDeleted = false AND s.status = 'ACTIVE' " +
           "GROUP BY s.yearLevel")
    List<Object[]> findActiveCountByYearLevel();

    // Department Analysis
    @Query("SELECT new com.hopesapms.app.modules.department.dto.DepartmentRetentionDTO(" +
           "d.name, " +
           "0.0, " + // Placeholder for retention rate (calculated in service)
           "COUNT(s), " +
           "SUM(CASE WHEN s.status IN ('WITHDRAWN', 'DISMISSED') THEN 1 ELSE 0 END), " +
           "'Low') " + // Placeholder for risk
           "FROM Student s JOIN s.department d " +
           "WHERE s.isDeleted = false " +
           "GROUP BY d.name")
    List<DepartmentRetentionDTO> getRawDepartmentRetentionStats();
}