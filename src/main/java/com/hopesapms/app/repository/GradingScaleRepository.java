package com.hopesapms.app.repository;

import com.hopesapms.app.model.GradingScale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GradingScaleRepository extends JpaRepository<GradingScale, Integer> {

    @Query("SELECT g FROM GradingScale g WHERE :score >= g.minScore AND :score <= g.maxScore")
    Optional<GradingScale> findByScore(Double score);
}