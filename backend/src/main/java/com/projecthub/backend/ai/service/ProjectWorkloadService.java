package com.projecthub.backend.ai.service;

import com.projecthub.backend.ai.dto.MemberWorkloadResponse;
import com.projecthub.backend.ai.dto.ProjectWorkloadResponse;
import com.projecthub.backend.dto.TaskAssignmentResponse;
import com.projecthub.backend.dto.TaskResponse;
import com.projecthub.backend.enums.TaskPriority;
import com.projecthub.backend.enums.TaskStatus;
import com.projecthub.backend.model.Project;
import com.projecthub.backend.model.ProjectMember;
import com.projecthub.backend.service.ProjectMemberService;
import com.projecthub.backend.service.ProjectService;
import com.projecthub.backend.service.TaskAssignmentService;
import com.projecthub.backend.service.TaskService;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ProjectWorkloadService {

    private final TaskAssignmentService taskAssignmentService;
    private final ProjectMemberService projectMemberService;
    private final ProjectService projectService;
    private final TaskService taskService;

    public ProjectWorkloadService(TaskAssignmentService taskAssignmentService, ProjectMemberService projectMemberService, ProjectService projectService, TaskService taskService) {
        this.taskAssignmentService = taskAssignmentService;
        this.projectMemberService = projectMemberService;
        this.projectService = projectService;
        this.taskService = taskService;
    }

    public ProjectWorkloadResponse getProjectWorkload(Long projectId)
    {
        //company owner or global admin or member
        Project project = projectService.getProjectById(projectId);
        List<TaskResponse> projectTasks = taskService.getTaskByProjectID(projectId);
        ProjectWorkloadResponse response = new ProjectWorkloadResponse();

        int totalTasks = projectTasks.size();
        response.setTotalTasks(totalTasks);
        response.setProjectId(project.getId());
        response.setProjectName(project.getName());

        List<TaskAssignmentResponse> taskAssignments = taskAssignmentService.getTaskAssignments(projectId);

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

        if(projectService.isOwnerOrGlobalAdmin(projectId)) {
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
        }
        return response;

    }

}
