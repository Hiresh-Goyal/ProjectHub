package com.projecthub.backend.mapper;

import com.projecthub.backend.dto.ProjectRequest;
import com.projecthub.backend.dto.ProjectResponse;
import com.projecthub.backend.model.Project;
import org.springframework.stereotype.Component;

@Component
public class ProjectMapper
{
    public ProjectResponse toProjectResponse(Project project) {
        return new ProjectResponse(project.getId(), project.getName(), project.getStatus());
    }

    public Project toProject(ProjectRequest projectRequest) {
        Project project = new Project();
        project.setName(projectRequest.getName());
        project.setStatus(projectRequest.getStatus());

        return project;
    }

    public void updateProject(ProjectRequest projectRequest, Project project) {
        project.setName(projectRequest.getName());
        project.setStatus(projectRequest.getStatus());
    }
}
