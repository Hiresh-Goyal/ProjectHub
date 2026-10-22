package com.projecthub.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateCompanyRequest {
    @NotBlank
    @Size(max = 150, message = "Company name cannot be greater than 150 characters")
    private String companyName;
}
