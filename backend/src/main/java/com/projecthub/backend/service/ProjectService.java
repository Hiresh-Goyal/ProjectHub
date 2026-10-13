package com.projecthub.backend.service;

import com.projecthub.backend.model.Project;
import com.projecthub.backend.repository.ProjectRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProjectService
{
    @Autowired
    ProjectRepo projectRepo;

    public List<Project> getProjects() {
        return projectRepo.getProjects();
    }

    public Project getProjectbyID(int id) {
        return projectRepo.getProjectbyID(id);
    }
}
