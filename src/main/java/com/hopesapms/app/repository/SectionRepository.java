package com.hopesapms.app.repository;

import com.hopesapms.app.model.Section;
import com.hopesapms.app.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SectionRepository extends JpaRepository<Section, Integer> {

    Optional<Section> findByIdAndIsDeletedFalse(Integer id);

     List<Section> findByIsDeletedFalse();

    List<Section> findByProgramIdAndIsDeletedFalse(Long programId);

    List<Section> findByProgramIdAndYearLevelAndIsDeletedFalse(Long programId, Integer yearLevel);

    Optional<Section> findByNameAndProgramIdAndYearLevelAndIsDeletedFalse(String name, Long programId, Integer yearLevel);

    Optional<Section> findByNameIgnoreCaseAndProgramId(String name, Long programId);

    @Query("""
           SELECT s FROM Student s
           WHERE s.section.id = :sectionId
             AND s.isDeleted = false
           """)
    List<Student> findStudentsBySectionId(Integer sectionId);

    @Query("SELECT s FROM Section s WHERE LOWER(s.name) = LOWER(?1) AND s.program.id = ?2 AND s.yearLevel = ?3 AND s.isDeleted = false")
    Optional<Section> findByNameIgnoreCaseAndProgramIdAndYearLevelAndIsDeletedFalse(String name, Long programId, Integer yearLevel);
}