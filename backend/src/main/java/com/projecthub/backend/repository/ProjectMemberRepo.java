package com.projecthub.backend.repository;

import com.projecthub.backend.model.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectMemberRepo extends JpaRepository<ProjectMember, Long> {
    boolean existsByProjectIdAndUserId(Long projectId, Long userId);
    List<ProjectMember> findAllByProjectId(Long projectId);
    List<ProjectMember> findAllByUserId(Long userId);
    Optional<ProjectMember> findByProjectIdAndUserId(Long projectId, Long userId);
    void deleteByProjectIdAndUserId(Long projectId, Long userId);
}
