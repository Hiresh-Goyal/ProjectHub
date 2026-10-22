package com.projecthub.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdatePasswordRequest
{
    @NotBlank(message = "Current password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Size(max = 50, message = "User Password cannot exceed 50 characters")
    private String currentPass;

    @NotBlank(message = "New password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Size(max = 50, message = "User Password cannot exceed 50 characters")
    private String newPass;
}
