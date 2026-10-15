package com.projecthub.backend.mapper;

import com.projecthub.backend.dto.CreateProjectRequest;
import com.projecthub.backend.dto.ProjectResponse;
import com.projecthub.backend.model.Project;
import org.springframework.stereotype.Component;

@Component
public class ProjectMapper
{
    public ProjectResponse mapToResponse(Project project) {
        return new ProjectResponse(project.getId(), project.getName(), project.getStatus());
    }

    public Project toEntity(CreateProjectRequest createProjectRequest) {
        Project project = new Project();
        project.setName(createProjectRequest.getName());
        project.setStatus(createProjectRequest.getStatus());

        return project;
    }

    public void updateEntity(CreateProjectRequest createProjectRequest, Project project) {
        project.setName(createProjectRequest.getName());
        project.setStatus(createProjectRequest.getStatus());
    }
}
