package com.projecthub.backend.service;

import com.projecthub.backend.model.Project;
import com.projecthub.backend.repository.ProjectRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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

    public Project getProjectbyID(long id) {
        return projectRepo.getProjectbyID(id);
    }

    public void addProject(Project project) {
        projectRepo.addProject(project);
    }

    public void updateProject(Project project) {
        projectRepo.updateProject(project);
    }
}
