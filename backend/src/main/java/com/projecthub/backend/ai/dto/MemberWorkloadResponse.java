package com.projecthub.backend.ai.dto;

import com.projecthub.backend.enums.TaskPriority;
import com.projecthub.backend.enums.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MemberWorkloadResponse {
    private Long projectMemberId;
    private Long userId;
    private String userName;
    private int assignedTaskCount;
    private int overdueTaskCount;
    private Map<TaskStatus,Integer> statusCounts;
    private Map<TaskPriority,Integer> priorityCounts;
}
