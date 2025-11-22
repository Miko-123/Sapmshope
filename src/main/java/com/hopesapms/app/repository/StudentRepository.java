package com.hopesapms.app.repository;

import com.hopesapms.app.model.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Integer> {

    boolean existsByStudentIdAndIsDeletedFalse(String studentId);

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
    Page<Student> findByYearLevelAndProgramIdAndIsDeletedFalse(Integer yearLevel, Integer programId, Pageable pageable);

    @EntityGraph(attributePaths = { "user", "program", "department" })
    List<Student> findByStudentIdInAndIsDeletedFalse(List<String> studentIds);

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
}