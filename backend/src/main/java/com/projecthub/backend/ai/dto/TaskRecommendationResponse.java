package com.projecthub.backend.ai.dto;

import com.projecthub.backend.dto.TaskResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskRecommendationResponse {
    private TaskResponse task;
    private String reason;
}
