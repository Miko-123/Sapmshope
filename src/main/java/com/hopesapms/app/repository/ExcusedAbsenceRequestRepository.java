package com.hopesapms.app.repository;

import com.hopesapms.app.model.ExcusedAbsenceRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ExcusedAbsenceRequestRepository extends JpaRepository<ExcusedAbsenceRequest, Integer> {
    @Query("SELECT ear FROM ExcusedAbsenceRequest ear WHERE ear.isDeleted = false")
    Page<ExcusedAbsenceRequest> findAllActive(Pageable pageable);

    @Query("SELECT ear FROM ExcusedAbsenceRequest ear JOIN FETCH ear.instructor WHERE ear.instructor.id = :instructorId AND ear.isDeleted = false")
    Page<ExcusedAbsenceRequest> findByInstructorId(Integer instructorId, Pageable pageable);

    @Query("SELECT ear FROM ExcusedAbsenceRequest ear WHERE ear.departmentId = :departmentId AND ear.isDeleted = false")
    Page<ExcusedAbsenceRequest> findByDepartmentId(Integer departmentId, Pageable pageable);

    @Query("SELECT COUNT(ear) FROM ExcusedAbsenceRequest ear WHERE ear.status = 'Pending' AND ear.isDeleted = false")
    Long countPendingRequests();
}