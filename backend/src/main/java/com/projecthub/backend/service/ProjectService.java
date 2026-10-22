package com.projecthub.backend.service;

import com.projecthub.backend.dto.ProjectRequest;
import com.projecthub.backend.dto.ProjectResponse;
import com.projecthub.backend.exception.ProjectNotFoundException;
import com.projecthub.backend.mapper.ProjectMapper;
import com.projecthub.backend.model.*;
import com.projecthub.backend.repository.ProjectMemberRepo;
import com.projecthub.backend.repository.ProjectRepo;
import com.projecthub.backend.repository.TaskAssignmentRepo;
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
    private final TaskAssignmentRepo  taskAssignmentRepo;

    public ProjectService(ProjectRepo projectRepo, ProjectMapper projectMapper, UserService userService, ProjectMemberRepo projectMemberRepo, TaskRepo taskRepo, TaskAssignmentRepo taskAssignmentRepo)
    {
        this.projectRepo = projectRepo;
        this.projectMapper = projectMapper;
        this.userService = userService;
        this.projectMemberRepo = projectMemberRepo;
        this.taskRepo = taskRepo;
        this.taskAssignmentRepo = taskAssignmentRepo;
    }

    public List<Project> getProjects() {
        User currentUser = userService.getLoggedInUser();
        if(userService.isGlobalAdmin())
            return projectRepo.findAll();

        Company company = currentUser.getCompany();
        if(userService.isCompanyOwner() && company != null)
        {
            return projectRepo.findAllByCompany(company);
        }

        if(currentUser.getCompany() == null)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not belong to a company hence you have no projects");

        List<ProjectMember> projectMembers = projectMemberRepo.findAllByUserId(currentUser.getId());
        List<Project> memberProjects = projectMembers
                .stream()
                .map(ProjectMember::getProject)
                .filter(project -> sameCompany(project,currentUser))
                .toList();

        List<Project> ownerProjects = projectRepo.findAllByOwner(currentUser);
        List<Project> allProjects = new ArrayList<>(memberProjects);
        ownerProjects = ownerProjects.stream()
                        .filter(project -> sameCompany(project,currentUser))
                        .toList();
        allProjects.addAll(ownerProjects);
        return allProjects;
    }

    public boolean sameCompany(Project project, User user)
    {
        if(user.getCompany() == null)
            return false;
        return Objects.equals(user.getCompany().getId(), project.getCompany().getId());
    }

    public Project getProjectById(long id) {
        Project project = projectRepo.findById(id).orElseThrow(() ->
                new ProjectNotFoundException("Project Not Found with ID : " + id));


        if(isOwnerOrMemberOrGlobalAdmin(project))
            return project;
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to access this project");
    }

    public boolean isProjectOwner(long projectId) {
        Project project = projectRepo.findById(projectId).orElseThrow(() ->
                new ProjectNotFoundException("Project Not Found with ID : " + projectId));
        User currentUser = userService.getLoggedInUser();
        return Objects.equals(project.getOwner().getId(), currentUser.getId());
    }

    public ProjectResponse addProject(ProjectRequest projectRequest) {

        Project project = projectMapper.toProject(projectRequest);
        User user =  userService.getLoggedInUser();
        if(user.getCompany() == null || user.getCompanyRole() == null)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not part of a company");
        project.setOwner(user);
        project.setCompany(user.getCompany());
        Project savedProject = projectRepo.save(project);

        return projectMapper.toProjectResponse(savedProject);
    }

    @Transactional
    public ProjectResponse updateProject(ProjectRequest projectRequest, long id) {
        if(isOwnerOrGlobalAdmin(id)) {
            Project existingProject = getProjectById(id);
            projectMapper.updateProject(projectRequest, existingProject);
            return projectMapper.toProjectResponse(existingProject);
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to update this project");
    }

    @Transactional
    public void deleteProject(long id) {
        if(isOwnerOrGlobalAdmin(id)) {
            List<Task> tasks = taskRepo.findByProjectId(id);
            //delete all task assignments
            tasks.forEach(task -> taskAssignmentRepo.deleteByTaskId(task.getId()));
            //delete all tasks
            taskRepo.deleteAll(tasks);
            //delete all member project relations
            projectMemberRepo.deleteAll(projectMemberRepo.findAllByProjectId(id));
            //delete the project
            projectRepo.deleteById(id);
        }
        else
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to delete this project");
    }

    public boolean isOwnerOrMemberOrGlobalAdmin(Project project)
    {
        User currentUser = userService.getLoggedInUser();
        if((currentUser.getCompany() == null) &&(!userService.isGlobalAdmin()))
            return false;


        return ((((isProjectOwner(project.getId())) ||
                (projectMemberRepo.existsByProjectIdAndUserId(project.getId(), currentUser.getId())) ||
                (userService.isCompanyOwner())) && sameCompany(project, currentUser)) ||
                (userService.isGlobalAdmin()));
    }

    public boolean isOwnerOrGlobalAdmin(Long projectId)
    {
        Project project = getProjectById(projectId);
        User currentUser = userService.getLoggedInUser();

        if((currentUser.getCompany() == null) &&(!userService.isGlobalAdmin()))
            return false;

        return ((((isProjectOwner(project.getId())) || (userService.isCompanyOwner()))
                && sameCompany(project, currentUser)) ||
                (userService.isGlobalAdmin()));
    }
}
