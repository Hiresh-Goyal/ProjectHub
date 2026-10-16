package com.projecthub.backend.service;

import com.projecthub.backend.dto.ProjectRequest;
import com.projecthub.backend.dto.ProjectResponse;
import com.projecthub.backend.exception.ProjectNotFoundException;
import com.projecthub.backend.mapper.ProjectMapper;
import com.projecthub.backend.model.Project;
import com.projecthub.backend.repository.ProjectRepo;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProjectService
{
    private final ProjectRepo projectRepo;
    private final ProjectMapper projectMapper;

    public ProjectService(ProjectRepo projectRepo, ProjectMapper projectMapper)
    {
        this.projectRepo = projectRepo;
        this.projectMapper = projectMapper;
    }

    public List<Project> getProjects() {
        return projectRepo.findAll();
    }

    public Project getProjectbyID(long id) {
        return projectRepo.findById(id).orElseThrow(() ->
                new ProjectNotFoundException("Project Not Found with ID : " + id));
    }

    public ProjectResponse addProject(ProjectRequest projectRequest) {

        Project project = projectMapper.toEntity(projectRequest);
        Project savedProject = projectRepo.save(project);

        return projectMapper.toProjectResponse(savedProject);
    }

    @Transactional
    public ProjectResponse updateProject(ProjectRequest projectRequest, long id) {
        Project existingProject = getProjectbyID(id);
        projectMapper.updateEntity(projectRequest, existingProject);
        return  projectMapper.toProjectResponse(existingProject);
    }

    public void deleteProject(long id) {
        projectRepo.deleteById(id);
    }
}
