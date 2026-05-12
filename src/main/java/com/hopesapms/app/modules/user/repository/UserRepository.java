package com.hopesapms.app.modules.user.repository;

import com.hopesapms.app.modules.user.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByUsernameAndIsDeletedFalse(String username);

    Optional<User> findByEmailAndIsDeletedFalse(String email);

    boolean existsByUsernameAndIsDeletedFalse(String username);
    
    boolean existsByEmailAndIsDeletedFalse(String email);

    boolean existsByUsernameAndIsDeletedFalseAndIdNot(String username, Integer id);

    @EntityGraph(attributePaths = {"roles"})
    Page<User> findByIsDeletedFalse(Pageable pageable);

    Optional<User> findByIdAndIsDeletedFalse(Integer id);

    @Query("SELECT u FROM User u JOIN u.roles r " +
           "WHERE r.name = :roleName AND u.department IS NULL AND u.isDeleted = false")
    List<User> findUnassignedByRole(String roleName);

    @EntityGraph(attributePaths = {"roles"})
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = :roleName AND u.isDeleted = false")
    Page<User> findByRoleName(String roleName, Pageable pageable);

    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = :roleName AND u.department.id = :departmentId AND u.isDeleted = false")
    List<User> findByDepartmentIdAndRoleName(@Param("departmentId") Long departmentId, @Param("roleName") String roleName);

    @Query("SELECT COUNT(u) FROM User u WHERE u.isDeleted = false AND SIZE(u.roles) > 0")
    Long countActiveUsersWithRoles();

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.isDeleted = true WHERE u.id IN :ids")
    void softDeleteByIds(Iterable<Integer> ids);
}