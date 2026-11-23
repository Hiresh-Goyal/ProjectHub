package com.projecthub.backend.repository;

import com.projecthub.backend.model.Company;
import com.projecthub.backend.model.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepo extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);

    @EntityGraph(attributePaths = "roles")
    Optional<User> findByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("Select u from User u where u.email = :email")
    Optional<User> findByEmailForOwnerTransfer(@Param("email") String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("Select u from User u where u.id = :id")
    Optional<User> findByIdForOwnerTransfer(@Param("id") Long id);

    List<User> findAllByCompany(Company company);
    List<User> findDistinctByRoles_Name(String role_name);
}
