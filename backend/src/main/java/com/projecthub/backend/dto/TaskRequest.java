package com.projecthub.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class TaskRequest
{
    @NotBlank(message = "Task title is required")
    @Size(max = 150, message = "Task title cannot exceed 150 characters")
    private String title;
    @Size(max = 1000, message = "Task description cannot exceed 1000 characters")
    private String description;
    @NotBlank(message = "Task status is required")
    private String status;
    @NotBlank(message = "Task priority is required")
    private String priority;
    private LocalDate dueDate;
    @NotNull(message = "Project Id is required")
    private Long projectId;
}
