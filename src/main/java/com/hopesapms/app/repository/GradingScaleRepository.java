package com.hopesapms.app.repository;

import com.hopesapms.app.model.GradingScale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface GradingScaleRepository extends JpaRepository<GradingScale, Integer> {
    @Query("SELECT g FROM GradingScale g WHERE g.isDeleted = false")
    Page<GradingScale> findAllActive(Pageable pageable);

    @Query("SELECT g FROM GradingScale g WHERE g.isActive = true AND g.isDeleted = false")
    GradingScale findActiveScale();
}