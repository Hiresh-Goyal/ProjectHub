package com.projecthub.backend.service;

import com.projecthub.backend.dto.ProjectMemberRequest;
import com.projecthub.backend.dto.ProjectMemberResponse;
import com.projecthub.backend.exception.UserNotFoundException;
import com.projecthub.backend.mapper.ProjectMemberMapper;
import com.projecthub.backend.model.Project;
import com.projecthub.backend.model.ProjectMember;
import com.projecthub.backend.model.User;
import com.projecthub.backend.repository.ProjectMemberRepo;
import com.projecthub.backend.repository.UserRepo;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ProjectMemberService
{
    private final UserService userService;
    private final UserRepo userRepo;
    private final ProjectService projectService;
    private final ProjectMemberRepo  projectMemberRepo;
    private final ProjectMemberMapper projectMemberMapper;

    public ProjectMemberService(UserService userService, UserRepo userRepo, ProjectService projectService, ProjectMemberRepo projectMemberRepo, ProjectMemberMapper projectMemberMapper) {
        this.userService = userService;
        this.userRepo = userRepo;
        this.projectService = projectService;
        this.projectMemberRepo = projectMemberRepo;
        this.projectMemberMapper = projectMemberMapper;
    }

    public ProjectMemberResponse addProjectMember(ProjectMemberRequest projectMemberRequest, Long projectId)
    {
        if(projectService.isProjectOwnerOrAdmin(projectId)) {
            ProjectMember projectMember = new ProjectMember();
            User user = userRepo.findById(projectMemberRequest.getUserId()).orElseThrow( () ->
                    new UserNotFoundException("User not found with id : " + projectMemberRequest.getUserId())
            );
            Project project = projectService.getProjectById(projectId);
            projectMember.setUser(user);
            projectMember.setProject(project);

            if(project.getOwner().getId().equals(user.getId()))
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Project Owner cannot be member of the project");

            if(projectMemberRepo.existsByProjectIdAndUserId(projectId, projectMemberRequest.getUserId()))
                throw new ResponseStatusException(HttpStatus.CONFLICT, "User is already member of the project");


            ProjectMember savedMember = projectMemberRepo.save(projectMember);
            return projectMemberMapper.toProjectMemberResponse(savedMember);
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to access this project");
    }

    public List<ProjectMember> getProjectMembers(Long projectId)
    {
        if(projectService.isProjectOwnerOrAdmin(projectId)) {
            return projectMemberRepo.findAllByProjectId(projectId);
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to access this project");
    }

    public ProjectMember getProjectMember(Long projectId, Long userId)
    {
        Project project = projectService.getProjectById(projectId);
        User currentUser = userService.getLoggedInUser();
        if(userService.isAdmin() || currentUser.getId().equals(userId) || project.getOwner().getId().equals(currentUser.getId()))
            return projectMemberRepo.findByProjectIdAndUserId(projectId,userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "The user " + userId + " is not a member in project " + projectId));

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to get other members of project");
    }

    private void isProjectMember(Long projectId, Long userId)
    {
        if(!projectMemberRepo.existsByProjectIdAndUserId(projectId,userId))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "The user " + userId + " is not a member in project " + projectId);
    }

    public void removeProjectMember(Long projectId, Long userId)
    {
        if(projectService.isProjectOwnerOrAdmin(projectId)) {
            isProjectMember(projectId, userId);
            projectMemberRepo.deleteByProjectIdAndUserId(projectId,userId);
        }
        else
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to remove a member");
    }

    public List<ProjectMember> getUserProjects()
    {
        User user = userService.getLoggedInUser();
        return projectMemberRepo.findAllByUserId(user.getId());
    }
}
