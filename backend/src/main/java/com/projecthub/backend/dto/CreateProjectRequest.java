package com.projecthub.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateProjectRequest
{
    @NotBlank(message = "Project name is required")
    @Size(max = 100, message = "Project name should not exceed 100 char")
    private String name;

    @NotBlank(message = "Project status cannot be blank")
    private String status;
}
