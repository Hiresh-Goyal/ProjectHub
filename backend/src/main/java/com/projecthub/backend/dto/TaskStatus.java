package com.projecthub.backend.dto;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TaskStatus {
    @NotNull(message = "Task status cannot be null")
    @Enumerated(EnumType.STRING)
    private com.projecthub.backend.enums.TaskStatus status;
}
