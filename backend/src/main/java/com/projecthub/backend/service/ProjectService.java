package com.projecthub.backend.service;

import com.projecthub.backend.dto.ProjectRequest;
import com.projecthub.backend.dto.ProjectResponse;
import com.projecthub.backend.exception.ProjectNotFoundException;
import com.projecthub.backend.mapper.ProjectMapper;
import com.projecthub.backend.model.Project;
import com.projecthub.backend.model.ProjectMember;
import com.projecthub.backend.model.User;
import com.projecthub.backend.repository.ProjectMemberRepo;
import com.projecthub.backend.repository.ProjectRepo;
import com.projecthub.backend.repository.TaskRepo;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class ProjectService
{
    private final ProjectRepo projectRepo;
    private final ProjectMapper projectMapper;
    private final UserService userService;
    private final ProjectMemberRepo projectMemberRepo;
    private final TaskRepo taskRepo;

    public ProjectService(ProjectRepo projectRepo, ProjectMapper projectMapper, UserService userService, ProjectMemberRepo projectMemberRepo, TaskRepo taskRepo)
    {
        this.projectRepo = projectRepo;
        this.projectMapper = projectMapper;
        this.userService = userService;
        this.projectMemberRepo = projectMemberRepo;
        this.taskRepo = taskRepo;
    }

    public List<Project> getProjects() {
        User currentUser = userService.getLoggedInUser();
        if(userService.isAdmin())
            return projectRepo.findAll();

        List<ProjectMember> projectMembers = projectMemberRepo.findAllByUserId(currentUser.getId());
        List<Project> memberProjects = projectMembers
                .stream()
                .map(ProjectMember::getProject)
                .toList();

        List<Project> ownerProjects = projectRepo.findAllByOwner(currentUser);
        List<Project> allProjects = new ArrayList<>(memberProjects);
        allProjects.addAll(ownerProjects);
        return allProjects;
    }

    public Project getProjectById(long id) {
        Project project = projectRepo.findById(id).orElseThrow(() ->
                new ProjectNotFoundException("Project Not Found with ID : " + id));

        User user = userService.getLoggedInUser();

        if(isProjectOwnerOrAdmin(id) || projectMemberRepo.existsByProjectIdAndUserId(id, user.getId()))
            return project;
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to access this project");
    }

    public boolean isProjectOwnerOrAdmin(long projectId) {
        Project project = projectRepo.findById(projectId).orElseThrow(() ->
                new ProjectNotFoundException("Project Not Found with ID : " + projectId));
        User currentUser = userService.getLoggedInUser();
        return Objects.equals(project.getOwner().getId(), currentUser.getId()) ||
                (currentUser.getRoles().stream().anyMatch(role -> role.getName().equals("ROLE_ADMIN")));
    }

    public ProjectResponse addProject(ProjectRequest projectRequest) {

        Project project = projectMapper.toProject(projectRequest);
        project.setOwner(userService.getLoggedInUser());
        Project savedProject = projectRepo.save(project);

        return projectMapper.toProjectResponse(savedProject);
    }

    @Transactional
    public ProjectResponse updateProject(ProjectRequest projectRequest, long id) {
        if(isProjectOwnerOrAdmin(id)) {
            Project existingProject = getProjectById(id);
            projectMapper.updateProject(projectRequest, existingProject);
            return projectMapper.toProjectResponse(existingProject);
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to update this project");
    }

    @Transactional
    public void deleteProject(long id) {
        if(isProjectOwnerOrAdmin(id)) {
            taskRepo.deleteAll(taskRepo.findByProjectId(id));
            projectMemberRepo.deleteAll(projectMemberRepo.findAllByProjectId(id));
            projectRepo.deleteById(id);
        }
        else
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to delete this project");
    }
}
