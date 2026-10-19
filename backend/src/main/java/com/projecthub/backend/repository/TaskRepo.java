package com.projecthub.backend.repository;

import com.projecthub.backend.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepo extends JpaRepository<Task, Long> {
    List<Task> findByProjectOwnerId(Long id);
    List<Task> findByProjectId(Long id);
}
