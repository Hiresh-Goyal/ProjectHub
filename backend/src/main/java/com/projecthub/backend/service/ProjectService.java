package com.projecthub.backend.service;

import com.projecthub.backend.exception.ProjectNotFoundException;
import com.projecthub.backend.model.Project;
import com.projecthub.backend.repository.ProjectRepo;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProjectService
{
    private final ProjectRepo projectRepo;

    public ProjectService(ProjectRepo projectRepo)
    {
        this.projectRepo = projectRepo;
    }

    public List<Project> getProjects() {
        return projectRepo.findAll();
    }

    public Project getProjectbyID(long id) {
        return projectRepo.findById(id).orElseThrow(() ->
                new ProjectNotFoundException("Project Not Found with ID : " + id));
    }

    public Project addProject(Project project) {
        return projectRepo.save(project);
    }

    public void updateProject(Project project) {
        projectRepo.save(project);
    }

    public void deleteProject(long id) {
        projectRepo.deleteById(id);
    }
}
