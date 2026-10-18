package com.projecthub.backend.repository;

import com.projecthub.backend.model.Project;
import com.projecthub.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepo extends JpaRepository<Project,Long>
{
    List<Project> findAllByOwner(User owner);
}
