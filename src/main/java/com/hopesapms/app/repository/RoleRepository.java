package com.hopesapms.app.repository;

import com.hopesapms.app.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {
    Optional<Role> findByName(String name);

    @Query("SELECT r FROM Role r WHERE r.name LIKE %:name%")
    Optional<Role> searchByNamePartial(String name);
}