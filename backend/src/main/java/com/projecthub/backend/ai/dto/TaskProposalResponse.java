package com.projecthub.backend.ai.dto;

import com.projecthub.backend.enums.TaskPriority;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskProposalResponse {

    private String title;
    private String description;
    private TaskPriority priority;
    private LocalDate dueDate;
    private String rationale;

}
