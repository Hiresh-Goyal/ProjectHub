package com.projecthub.backend.ai.tools;

import com.projecthub.backend.ai.dto.RecommendationContextResponse;
import com.projecthub.backend.ai.service.ProjectRecommendationService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class ProjectRecommendationTools {

    private final ProjectRecommendationService projectRecommendationService;

    public ProjectRecommendationTools(ProjectRecommendationService projectRecommendationService) {
        this.projectRecommendationService = projectRecommendationService;
    }

    @Tool(name = "getRecommendationContext", description = """
            
            Retrieves the complete recommendation context for a specified project.
            
            Includes:
            - Project information.
            - All accessible tasks in the project.
            - Task assignment information available to the authenticated user.
            - Project workload statistics, subject to the user's permissions.
            - The authenticated user's recommendation eligibility scope.
            - The IDs of tasks eligible for recommendation.
            
            Recommendation eligibility rules:
            - Project owners, company administrators belonging to the project's company, and global administrators can receive recommendations for all incomplete project tasks, including unassigned tasks.
            - Ordinary project members can receive recommendations only for incomplete tasks assigned to themselves.
            - Ordinary members may also receive broader project task and assignment information as context.
            
            MUST be used when the user asks what to work on next, which tasks to prioritize, or requests task recommendations.
            
            Never recommend task IDs outside the backend-provided recommendableTaskIds collection. Never invent task information or assume access to information that was not returned.
            """)
    public RecommendationContextResponse getRecommendationContext(@ToolParam(description = "The unique ID of the project for which task recommendations are requested. Must identify a project accessible to the currently authenticated user. The backend validates project access and determines the user's recommendation eligibility scope.")
                                                                    Long projectId)
    {
        return projectRecommendationService.getRecommendationContext(projectId);
    }
}
