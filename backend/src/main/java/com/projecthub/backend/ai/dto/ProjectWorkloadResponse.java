package com.projecthub.backend.ai.dto;

import com.projecthub.backend.dto.TaskResponse;
import com.projecthub.backend.enums.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProjectWorkloadResponse {
    private Long projectId;
    private String projectName;
    private int totalTasks;
    private int assignedTasks;
    private int unassignedTasks;
    private int totalAssignments;
    private int completedTasks;
    private double completionPercentage;
    private int overdueTasks;
    private List<TaskResponse> overdueTaskDetails = new ArrayList<>();
    private Map<TaskStatus,Integer> overallStatusCounts;
    private List<MemberWorkloadResponse> members;
}
