package com.hopesapms.app.repository;

import com.hopesapms.app.model.Section;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SectionsRepository extends JpaRepository<Section, Integer> {
    List<Section> findByIsDeletedFalse();
    
}
