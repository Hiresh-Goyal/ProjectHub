package com.projecthub.backend.service;

import com.projecthub.backend.dto.ProjectRequest;
import com.projecthub.backend.dto.ProjectResponse;
import com.projecthub.backend.exception.ProjectNotFoundException;
import com.projecthub.backend.mapper.ProjectMapper;
import com.projecthub.backend.model.Project;
import com.projecthub.backend.model.User;
import com.projecthub.backend.repository.ProjectRepo;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;

@Service
public class ProjectService
{
    private final ProjectRepo projectRepo;
    private final ProjectMapper projectMapper;
    private final UserService userService;

    public ProjectService(ProjectRepo projectRepo, ProjectMapper projectMapper, UserService userService)
    {
        this.projectRepo = projectRepo;
        this.projectMapper = projectMapper;
        this.userService = userService;
    }

    public List<Project> getProjects() {
        User currentUser = userService.getLoggedInUser();
        return projectRepo.findAllByOwner(currentUser);
    }

    public Project getProjectById(long id) {
        Project project = projectRepo.findById(id).orElseThrow(() ->
                new ProjectNotFoundException("Project Not Found with ID : " + id));
        User currentUser = userService.getLoggedInUser();
        if(Objects.equals(project.getOwner().getId(), currentUser.getId()) ||
                (currentUser.getRoles().stream().anyMatch(role -> role.getName().equals("ROLE_ADMIN"))))
        {
            return project;
        }
        else
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to access this project");
    }

    public ProjectResponse addProject(ProjectRequest projectRequest) {

        Project project = projectMapper.toProject(projectRequest);
        project.setOwner(userService.getLoggedInUser());
        Project savedProject = projectRepo.save(project);

        return projectMapper.toProjectResponse(savedProject);
    }

    @Transactional
    public ProjectResponse updateProject(ProjectRequest projectRequest, long id) {
        Project existingProject = getProjectById(id);
        projectMapper.updateProject(projectRequest, existingProject);
        return projectMapper.toProjectResponse(existingProject);
    }

    public void deleteProject(long id) {
        projectRepo.deleteById(id);
    }
}
