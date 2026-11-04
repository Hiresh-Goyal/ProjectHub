package com.projecthub.backend.ai.tools;

import com.projecthub.backend.ai.dto.MemberWorkloadResponse;
import com.projecthub.backend.ai.dto.ProjectWorkloadResponse;
import com.projecthub.backend.dto.ProjectMemberResponse;
import com.projecthub.backend.dto.ProjectResponse;
import com.projecthub.backend.dto.TaskAssignmentResponse;
import com.projecthub.backend.dto.TaskResponse;
import com.projecthub.backend.enums.TaskPriority;
import com.projecthub.backend.enums.TaskStatus;
import com.projecthub.backend.mapper.ProjectMapper;
import com.projecthub.backend.mapper.ProjectMemberMapper;
import com.projecthub.backend.mapper.TaskAssignmentMapper;
import com.projecthub.backend.mapper.TaskMapper;
import com.projecthub.backend.model.Project;
import com.projecthub.backend.model.ProjectMember;
import com.projecthub.backend.model.TaskAssignment;
import com.projecthub.backend.service.*;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.*;


@Component
public class ProjectTools {

    private final TaskAssignmentService taskAssgnmentService;
    private final TaskAssignmentMapper taskAssignmentMapper;
    private final ProjectMemberService projectMemberService;
    private final ProjectMemberMapper projectMemberMapper;
    private final ProjectService projectService;
    private final ProjectMapper projectMapper;
    private final TaskService taskService;
    private final TaskMapper taskMapper;


    public ProjectTools(TaskAssignmentService taskAssgnmentService, TaskAssignmentMapper taskAssignmentMapper, ProjectMemberService projectMemberService, ProjectMemberMapper projectMemberMapper, ProjectService projectService, ProjectMapper projectMapper, TaskService taskService, TaskMapper taskMapper) {
        this.taskAssgnmentService = taskAssgnmentService;
        this.taskAssignmentMapper = taskAssignmentMapper;
        this.projectMemberService = projectMemberService;
        this.projectMemberMapper = projectMemberMapper;
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
        List<TaskAssignment> taskAssignments = taskAssgnmentService.getAssignedTasks();
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
        List<TaskAssignment> taskAssignments = taskAssgnmentService.getTaskAssignees(taskId);
        return taskAssignments.stream().map(taskAssignmentMapper::toTaskAssignmentResponse).toList();
    }

    @Tool(description = """
    Retrieves project-level workload and analytics.

    Includes:
    - Total, assigned and unassigned tasks.
    - Completed tasks and completion percentage.
    - Number of overdue tasks.
    - Workload distribution across project members.
    - Individual member status, priority and overdue counts.

    MUST be used when the user asks about project workload,
    task distribution, project progress, completion statistics,
    or overdue tasks.

    The requesting user must have permission to access the project.
    """)
    public ProjectWorkloadResponse getProjectWorkload(@ToolParam(description = "ID of the project to analyse.")
                                       Long projectId)
    {
        //company owner or global admin or member
        Project project = projectService.getProjectById(projectId);
        List<TaskResponse> projectTasks = taskService.getTaskByProjectID(projectId);
        ProjectWorkloadResponse response = new ProjectWorkloadResponse();

        int totalTasks = projectTasks.size();
        response.setTotalTasks(totalTasks);
        response.setProjectId(project.getId());
        response.setProjectName(project.getName());

        List<TaskAssignmentResponse> taskAssignments = taskAssgnmentService.getTaskAssignments(projectId);

        response.setTotalAssignments(taskAssignments.size());

        //getting overall statistics of taskAssignments
        Map<TaskStatus, Integer> overallStatusCounts = new EnumMap<>(TaskStatus.class);

        for (TaskStatus status : TaskStatus.values()) {
            overallStatusCounts.put(status, 0);
        }

        for(TaskResponse taskResponse : projectTasks)
        {
            overallStatusCounts.merge(taskResponse.getStatus(), 1, Integer::sum);
            if(taskService.isTaskOverdue(taskResponse)) {
                response.setOverdueTasks(response.getOverdueTasks() + 1);
                response.getOverdueTaskDetails().add(taskResponse);
            }
        }
        Set<Long> assignedTaskIds = new HashSet<>();
        for(TaskAssignmentResponse taskAssignment : taskAssignments)
        {
            assignedTaskIds.add(taskAssignment.getTaskId());
        }

        response.setAssignedTasks(assignedTaskIds.size());
        response.setUnassignedTasks(totalTasks - assignedTaskIds.size());
        response.setOverallStatusCounts(overallStatusCounts);

        int completedTasks = overallStatusCounts.get(TaskStatus.COMPLETED);
        response.setCompletedTasks(completedTasks);
        if(totalTasks == 0)
            response.setCompletionPercentage(0);
        else
        {
            double completionPercentage = (double) (completedTasks * 100) /totalTasks;
            response.setCompletionPercentage(completionPercentage);
        }

        List<ProjectMember> projectMembers = projectMemberService.getProjectMembers(projectId);
        Map<Long, MemberWorkloadResponse> membersById = new HashMap<>();
        for (ProjectMember projectMember : projectMembers) {

            Map<TaskStatus, Integer> statusCounts = new EnumMap<>(TaskStatus.class);

            for (TaskStatus status : TaskStatus.values()) {
                statusCounts.put(status, 0);
            }

            Map<TaskPriority, Integer> priorityCounts = new EnumMap<>(TaskPriority.class);

            for (TaskPriority priority : TaskPriority.values()) {
                priorityCounts.put(priority, 0);
            }

            MemberWorkloadResponse memberResponse = new MemberWorkloadResponse();

            memberResponse.setProjectMemberId(projectMember.getId());
            memberResponse.setUserId(projectMember.getUser().getId());
            memberResponse.setUserName(projectMember.getUser().getName());

            memberResponse.setAssignedTaskCount(0);
            memberResponse.setStatusCounts(statusCounts);
            memberResponse.setPriorityCounts(priorityCounts);

            membersById.put(projectMember.getId(), memberResponse);
        }

        Map<Long, TaskResponse> tasksById = new HashMap<>();

        for (TaskResponse task : projectTasks) {
            tasksById.put(task.getId(), task);
        }
        for (TaskAssignmentResponse assignment : taskAssignments) {

            // 1. Find the member
            MemberWorkloadResponse member =
                    membersById.get(assignment.getProjectMemberId());

            // 2. Find the task
            TaskResponse taskResponse =
                    tasksById.get(assignment.getTaskId());

            member.setAssignedTaskCount(
                    member.getAssignedTaskCount() + 1
            );

            TaskStatus status = taskResponse.getStatus();

            member.getStatusCounts().put(
                    status,
                    member.getStatusCounts().get(status) + 1
            );

            TaskPriority priority = taskResponse.getPriority();

            member.getPriorityCounts().put(
                    priority,
                    member.getPriorityCounts().get(priority) + 1
            );
            if(taskService.isTaskOverdue(taskResponse))
                member.setOverdueTaskCount(member.getOverdueTaskCount() + 1);

        }

        response.setMembers(new ArrayList<>(membersById.values()));

        return response;

    }
}
