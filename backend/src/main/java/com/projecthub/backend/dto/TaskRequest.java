package com.projecthub.backend.dto;

import com.projecthub.backend.enums.TaskPriority;
import com.projecthub.backend.enums.TaskStatus;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
    @NotNull(message = "Task status cannot be null")
    @Enumerated(EnumType.STRING)
    private TaskStatus status;
    @NotNull(message = "Task priority cannot be null")
    @Enumerated(EnumType.STRING)
    private TaskPriority priority;
    private LocalDate dueDate;
    @NotNull(message = "Project Id is required")
    @Positive
    private Long projectId;
}
