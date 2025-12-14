package com.hopesapms.app.repository;

import com.hopesapms.app.model.GradeChangeRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GradeChangeRequestRepository extends JpaRepository<GradeChangeRequest, Long> {

    Page<GradeChangeRequest> findByRequestedBy_Id(Integer userId, Pageable pageable);

    Page<GradeChangeRequest> findByStatus(String status, Pageable pageable);

    @Query("SELECT g FROM GradeChangeRequest g WHERE g.enrollment.courseOffering.id = :offeringId")
    List<GradeChangeRequest> findByOfferingId(Long offeringId);
}