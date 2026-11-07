package com.projecthub.backend.ai.dto;

import com.projecthub.backend.ai.enums.RecommendationEligibilityScope;
import com.projecthub.backend.dto.ProjectResponse;
import com.projecthub.backend.dto.TaskAssignmentResponse;
import com.projecthub.backend.dto.TaskResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecommendationContextResponse {
    private ProjectResponse project;
    private RecommendationEligibilityScope eligibilityScope;
    private Long currentUserId;
    private LocalDate analysisDate;
    private List<TaskResponse> projectTasks;
    private List<TaskAssignmentResponse> taskAssignments;
    private Set<Long> recommendableTaskIds;
    private ProjectWorkloadResponse projectWorkload;
}
