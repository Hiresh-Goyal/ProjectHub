package com.projecthub.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskAssignmentResponse {
    private Long id;
    private Long taskId;
    private Long projectMemberId;
    private Long userId;
    private String taskName;
    private String projectName;
    private String userName;
    private LocalDateTime createdAt;
}
