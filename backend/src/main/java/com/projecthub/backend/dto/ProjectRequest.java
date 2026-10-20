package com.projecthub.backend.dto;

import com.projecthub.backend.enums.ProjectStatus;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProjectRequest
{
    @NotBlank(message = "Project name is required")
    @Size(max = 100, message = "Project name should not exceed 100 char")
    private String name;

    @NotNull(message = "Project status cannot be null")
    @Enumerated(EnumType.STRING)
    private ProjectStatus status;

}
