package com.projecthub.backend.service;

import com.projecthub.backend.dto.ProjectMemberRequest;
import com.projecthub.backend.dto.ProjectMemberResponse;
import com.projecthub.backend.exception.UserNotFoundException;
import com.projecthub.backend.mapper.ProjectMemberMapper;
import com.projecthub.backend.model.Project;
import com.projecthub.backend.model.ProjectMember;
import com.projecthub.backend.model.User;
import com.projecthub.backend.repository.ProjectMemberRepo;
import com.projecthub.backend.repository.TaskAssignmentRepo;
import com.projecthub.backend.repository.UserRepo;
import jakarta.transaction.Transactional;
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
    private final TaskAssignmentRepo  taskAssignmentRepo;

    public ProjectMemberService(UserService userService, UserRepo userRepo, ProjectService projectService, ProjectMemberRepo projectMemberRepo, ProjectMemberMapper projectMemberMapper, TaskAssignmentRepo taskAssignmentRepo) {
        this.userService = userService;
        this.userRepo = userRepo;
        this.projectService = projectService;
        this.projectMemberRepo = projectMemberRepo;
        this.projectMemberMapper = projectMemberMapper;
        this.taskAssignmentRepo = taskAssignmentRepo;
    }

    public ProjectMemberResponse addProjectMember(ProjectMemberRequest projectMemberRequest, Long projectId)
    {
        if(projectService.isOwnerOrGlobalAdmin(projectId)) {
            ProjectMember projectMember = new ProjectMember();
            User user = userRepo.findById(projectMemberRequest.getUserId()).orElseThrow( () ->
                    new UserNotFoundException("User not found with id : " + projectMemberRequest.getUserId())
            );
            Project project = projectService.getProjectById(projectId);
            if(user.getCompany() == null)
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,"user does not belong to a company");
            if(!projectService.sameCompany(project, user))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Project member is not in same company");
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
        if(projectService.isOwnerOrGlobalAdmin(projectId)){
            projectService.getProjectById(projectId);
            return projectMemberRepo.findAllByProjectId(projectId);
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to access this project");
    }

    public ProjectMember getProjectMember(Long projectId, Long userId)
    {
        projectService.getProjectById(projectId);
        User currentUser = userService.getLoggedInUser();
        if(projectService.isOwnerOrGlobalAdmin(projectId) || currentUser.getId().equals(userId))
            return projectMemberRepo.findByProjectIdAndUserId(projectId,userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "The user " + userId + " is not a member in project " + projectId));

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to get other members of project");
    }

    @Transactional
    public void removeProjectMember(Long projectId, Long userId)
    {
        if(projectService.isOwnerOrGlobalAdmin(projectId)) {
            projectService.getProjectById(projectId);
            ProjectMember projectMember = getProjectMember(projectId, userId);
            taskAssignmentRepo.deleteByProjectMemberId(projectMember.getId());
            projectMemberRepo.deleteByProjectIdAndUserId(projectId,userId);
        }
        else
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to remove a member");
    }

    public List<ProjectMember> getUserProjects()
    {
        User currentUser = userService.getLoggedInUser();
        if(currentUser.getCompany() == null)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not part of any company hence you have no projects");

        return projectMemberRepo.findAllByUserId(currentUser.getId()).stream()
                .filter(projectMember -> projectService.sameCompany(projectMember.getProject(), currentUser))
                .toList();
    }

    public ProjectMember getMemberByProjectMemberId(Long projectMemberId)
    {
        return projectMemberRepo.findById(projectMemberId).orElseThrow( () ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "The projectMember id :  " + projectMemberId + " does not exist"));

    }
}
