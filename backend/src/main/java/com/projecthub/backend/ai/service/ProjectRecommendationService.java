package com.projecthub.backend.ai.service;

import com.projecthub.backend.ai.dto.RecommendationContextResponse;
import com.projecthub.backend.ai.dto.TaskRecommendationDraft;
import com.projecthub.backend.ai.dto.TaskRecommendationResponse;
import com.projecthub.backend.ai.enums.RecommendationEligibilityScope;
import com.projecthub.backend.dto.TaskResponse;
import com.projecthub.backend.enums.TaskStatus;
import com.projecthub.backend.mapper.ProjectMapper;
import com.projecthub.backend.service.ProjectService;
import com.projecthub.backend.service.TaskAssignmentService;
import com.projecthub.backend.service.TaskService;
import com.projecthub.backend.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProjectRecommendationService {

    private final ProjectWorkloadService projectWorkloadService;
    private final TaskAssignmentService taskAssignmentService;
    private final ProjectService projectService;
    private final ProjectMapper projectMapper;
    private final TaskService taskService;
    private final UserService userService;

    public ProjectRecommendationService(ProjectWorkloadService projectWorkloadService, TaskAssignmentService taskAssignmentService, ProjectService projectService, ProjectMapper projectMapper, TaskService taskService, UserService userService) {
        this.projectWorkloadService = projectWorkloadService;
        this.taskAssignmentService = taskAssignmentService;
        this.projectService = projectService;
        this.projectMapper = projectMapper;
        this.taskService = taskService;
        this.userService = userService;
    }

    public RecommendationEligibilityScope resolveScope(Long projectId)
    {
        projectService.getProjectById(projectId);
        if(projectService.isOwnerOrGlobalAdmin(projectId))
            return RecommendationEligibilityScope.PROJECT_WIDE;

        return RecommendationEligibilityScope.PERSONAL;
    }

    public RecommendationContextResponse getRecommendationContext(Long projectId)
    {
        RecommendationContextResponse response = new RecommendationContextResponse();
        response.setProject(projectMapper.toProjectResponse(projectService.getProjectById(projectId)));
        response.setEligibilityScope(resolveScope(projectId));
        response.setCurrentUserId(userService.getLoggedInUser().getId());
        response.setAnalysisDate(LocalDate.now(ZoneId.of("Asia/Kolkata")));
        response.setProjectTasks(taskService.getTaskByProjectID(projectId));
        response.setTaskAssignments(taskAssignmentService.getTaskAssignments(projectId));

        if(response.getEligibilityScope() == RecommendationEligibilityScope.PROJECT_WIDE)
        {
            response.setRecommendableTaskIds(response.getProjectTasks()
                    .stream()
                    .filter(taskResponse -> taskResponse.getStatus()!= TaskStatus.COMPLETED)
                    .map(TaskResponse::getId)
                    .collect(Collectors.toSet()));
        }
        else
        {
            response.setRecommendableTaskIds(taskService.getMyAssignedTasksInProject(projectId)
                    .stream()
                    .filter(taskResponse -> taskResponse.getStatus()!= TaskStatus.COMPLETED)
                    .map(TaskResponse::getId)
                    .collect(Collectors.toSet()));
        }
        response.setProjectWorkload(projectWorkloadService.getProjectWorkload(projectId));

        return response;
    }


    public List<TaskRecommendationResponse> validateAndBuildRecommendations(
            Long projectId,
            List<TaskRecommendationDraft> recommendations
    ) {
        // 1. Handle invalid or empty AI output
        if (recommendations == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "AI returned an invalid recommendation response"
            );
        }

        if (recommendations.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. Retrieve fresh, backend-authorized recommendation context
        RecommendationContextResponse contextResponse =
                getRecommendationContext(projectId);

        Set<Long> recommendableTaskIds =
                contextResponse.getRecommendableTaskIds();

        // 3. Build a lookup of actual project tasks
        Map<Long, TaskResponse> tasksById = new HashMap<>();

        for (TaskResponse task : contextResponse.getProjectTasks()) {
            tasksById.put(task.getId(), task);
        }

        // 4. Track IDs to detect duplicate recommendations
        Set<Long> processedTaskIds = new HashSet<>();

        // 5. Store validated recommendations
        List<TaskRecommendationResponse> validatedRecommendations =
                new ArrayList<>();

        for (TaskRecommendationDraft recommendation : recommendations) {

            // Check that the recommendation and its fields are valid
            if (recommendation == null
                    || recommendation.getTaskId() == null
                    || recommendation.getReason() == null
                    || recommendation.getReason().isBlank()) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "AI generated an invalid task recommendation"
                );
            }

            Long taskId = recommendation.getTaskId();

            // Check for duplicate task IDs
            if (!processedTaskIds.add(taskId)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "AI generated duplicate task recommendations"
                );
            }

            // Check recommendation eligibility
            if (recommendableTaskIds == null
                    || !recommendableTaskIds.contains(taskId)) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "AI recommended a task that is not eligible"
                );
            }

            // Retrieve actual task information from ProjectHub
            TaskResponse task = tasksById.get(taskId);

            // Verify that the task exists in this project and is incomplete
            if (task == null
                    || !projectId.equals(task.getProjectId())
                    || task.getStatus() == TaskStatus.COMPLETED) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "AI recommended an invalid or completed task"
                );
            }

            // Construct the validated recommendation
            TaskRecommendationResponse validatedRecommendation =
                    new TaskRecommendationResponse(
                            task,
                            recommendation.getReason().strip()
                    );

            validatedRecommendations.add(validatedRecommendation);
        }

        return validatedRecommendations;
    }

}
