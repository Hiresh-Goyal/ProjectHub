package com.projecthub.backend.repository;

import com.projecthub.backend.model.TaskAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskAssignmentRepo extends JpaRepository<TaskAssignment, Long> {
    boolean existsByTaskIdAndProjectMemberId(Long taskId, Long projectMemberId);
    boolean existsByTaskId(Long taskId);
    List<TaskAssignment> findAllByTaskId(Long taskId);
    List<TaskAssignment> findAllByProjectMemberId(Long projectMemberId);
    void deleteByTaskId(Long taskId);
    void deleteByProjectMemberId(Long projectMemberId);
}
