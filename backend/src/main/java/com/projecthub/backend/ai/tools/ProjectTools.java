package com.projecthub.backend.ai.tools;

import com.projecthub.backend.ai.dto.ProjectWorkloadResponse;
import com.projecthub.backend.ai.service.ProjectWorkloadService;
import com.projecthub.backend.dto.*;
import com.projecthub.backend.mapper.*;
import com.projecthub.backend.model.Project;
import com.projecthub.backend.model.ProjectMember;
import com.projecthub.backend.model.TaskAssignment;
import com.projecthub.backend.model.User;
import com.projecthub.backend.service.*;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
public class ProjectTools {
    private final ProjectWorkloadService projectWorkloadService;
    private final TaskAssignmentService taskAssignmentService;
    private final TaskAssignmentMapper taskAssignmentMapper;
    private final ProjectMemberService projectMemberService;
    private final ProjectMemberMapper projectMemberMapper;
    private final UserService userService;
    private final UserMapper userMapper;
    private final ProjectService projectService;
    private final ProjectMapper projectMapper;
    private final TaskService taskService;
    private final TaskMapper taskMapper;


    public ProjectTools(ProjectWorkloadService projectWorkloadService, TaskAssignmentService taskAssignmentService, TaskAssignmentMapper taskAssignmentMapper, ProjectMemberService projectMemberService, ProjectMemberMapper projectMemberMapper, UserService userService, UserMapper userMapper, ProjectService projectService, ProjectMapper projectMapper, TaskService taskService, TaskMapper taskMapper) {
        this.projectWorkloadService = projectWorkloadService;
        this.taskAssignmentService = taskAssignmentService;
        this.taskAssignmentMapper = taskAssignmentMapper;
        this.projectMemberService = projectMemberService;
        this.projectMemberMapper = projectMemberMapper;
        this.userService = userService;
        this.userMapper = userMapper;
        this.projectService = projectService;
        this.projectMapper = projectMapper;
        this.taskService = taskService;
        this.taskMapper = taskMapper;
    }

    @Tool(name = "getProjects", description = """
            - It retrieves projects accessible to the currently authenticated user.
            - It respects the user's existing ProjectHub permissions.
            - It must be used when answering questions about the user's accessible projects.
            - It does not retrieve arbitrary projects based on an ID supplied by the model.""")
    public List<ProjectResponse> getProjects() {
        List<Project> myProjects = projectService.getProjects();
        return projectMapper.toProjectResponses(myProjects);
    }

    @Tool(name = "getProjectDetails", description = "Retrieves detailed information about a specific project using its project ID, including its name, status, and other available project details.")
    public ProjectResponse getProjectDetails(@ToolParam(description = "The ID of the project to retrieve") Long projectId)
    {
        Project project = projectService.getProjectById(projectId);
        return projectMapper.toProjectResponse(project);
    }

    @Tool(name = "getTaskByProjectId", description = """
    Retrieves all tasks belonging to a specific project.
    Use this tool when the user asks to view, list, or summarize
    tasks within a project. Requires a project ID.
    Returns task details such as title, description, status,
    priority, and assignee when available.
    """)
    public List<TaskResponse> getTasksByProjectID(@ToolParam(description = """
    The unique ID of the project whose tasks should be retrieved.
    Must be a valid project ID accessible to the authenticated user.
    """) Long projectId)
    {
        return taskService.getTaskByProjectID(projectId);
    }

    @Tool(description = """
    Retrieves detailed information about a specific task using its task ID.
    Use this tool when the user asks about an individual task's details,
    status, priority, description, or assignee.
    Returns the task's available information.
    """)
    public TaskResponse getTaskDetails(@ToolParam(description = """
    The unique ID of the task to retrieve.
    Must identify an existing task within a project accessible
    to the authenticated user.
    """) Long taskId)
    {
        return taskMapper.toTaskResponse(taskService.getTaskById(taskId));
    }

    @Tool(description = """
    Retrieves all tasks specifically assigned to the currently
    authenticated user across their accessible projects.
    Use this tool when the user asks about their assigned tasks,
    personal workload, or tasks they are responsible for.
    Returns available task details and their associated projects.
    """)
    public List<TaskAssignmentResponse> getAssignedTasks()
    {
        List<TaskAssignment> taskAssignments = taskAssignmentService.getAssignedTasks();
        return taskAssignments.stream().map(taskAssignmentMapper::toTaskAssignmentResponse).toList();
    }

    @Tool(description = """
    Retrieves the members of a specified project.
    Use this tool when the user asks about project membership,
    team composition, or who is part of a project.
    The requesting user must have permission to access the project.
    """)
    public List<ProjectMemberResponse> getProjectMembers(@ToolParam(description = "The ID of the project whose members should be retrieved.")
                                                             Long projectId)
    {
        List<ProjectMember> projectMembers = projectMemberService.getProjectMembers(projectId);
        return projectMembers.stream().map(projectMemberMapper::toProjectMemberResponse).toList();
    }

    @Tool(description = """
    Retrieves the users assigned to a specific task.
    Use this tool when the user asks who is responsible for a task,
    wants to see its assignees, or wants to verify task assignments.
    The requesting user must have permission to access the task.
    """)
    public List<TaskAssignmentResponse> getTaskAssignees(@ToolParam(description = "ID of the task whose assignees should be retrieved.")
                                     Long taskId)
    {
        List<TaskAssignment> taskAssignments = taskAssignmentService.getTaskAssignees(taskId);
        return taskAssignments.stream().map(taskAssignmentMapper::toTaskAssignmentResponse).toList();
    }

    @Tool(description = """
    Retrieves project-level workload statistics and analytics for a specified project.
    
    Includes:
    - Total number of tasks.
    - Assigned and unassigned task counts.
    - Completed tasks and completion percentage.
    - Number of overdue tasks and overdue task details.
    - Overall task status distribution.
    
    Individual member workload information is subject to the authenticated user's permissions:
    
    - Project owners, company administrators belonging to the project's company, and global administrators receive detailed member workload statistics, including assigned task counts, status distribution, priority distribution, and overdue task counts.
    - Ordinary project members receive aggregate project workload statistics only. Individual member workload details are not included.
    
    MUST be used when the user asks about project workload, task distribution, project progress, completion statistics, or overdue tasks.
    
    The backend validates project access and determines which workload information the authenticated user is authorized to receive. Never assume that an empty member workload collection means the project has no members.
    """)
    public ProjectWorkloadResponse getProjectWorkload(@ToolParam(description = "ID of the project to analyse.")
                                       Long projectId)
    {
        return projectWorkloadService.getProjectWorkload(projectId);
    }

    @Tool(name = "getLoggedInUser", description = """
            Retrieves details about the currently authenticated user in ProjectHub, including their user ID, name, email address, company ID, company name, company role, account creation timestamp, and last update timestamp. Use this tool when the user asks about their profile, account information, company affiliation, or role within their company. Returns information only about the currently logged-in user.""")
    public UserResponse getLoggedInUser()
    {
        User currentUser = userService.getLoggedInUser();
        return userMapper.toUserResponse(currentUser);
    }
}
