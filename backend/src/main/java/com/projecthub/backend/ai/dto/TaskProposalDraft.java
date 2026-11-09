package com.projecthub.backend.ai.dto;

import com.projecthub.backend.enums.TaskPriority;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskProposalDraft {
    @NotBlank(message = "Task title is required")
    @Size(max = 150, message = "Task title cannot exceed 150 characters")
    private String title;

    @Size(max = 1000, message = "Task description cannot exceed 1000 characters")
    private String description;

    @NotNull(message = "Task priority cannot be null")
    @Enumerated(EnumType.STRING)
    private TaskPriority priority;

    private LocalDate dueDate;

    @NotBlank(message = "Task reason is required")
    @Size(max = 500, message = "Task reason cannot exceed 500 characters")
    private String rationale;
}
