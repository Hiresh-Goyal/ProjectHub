package com.projecthub.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TaskStatus {
    @NotBlank(message = "Task status is required")
    private String status;
}
