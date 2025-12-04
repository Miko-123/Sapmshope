package com.hopesapms.app.repository;

import com.hopesapms.app.model.GradingScaleDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GradingScaleDetailRepository extends JpaRepository<GradingScaleDetail, Integer> {
    @Query("SELECT gsd FROM GradingScaleDetail gsd JOIN FETCH gsd.gradingScale gs WHERE gs.id = :gradingScaleId")
    List<GradingScaleDetail> findByGradingScaleId(Integer gradingScaleId);
}