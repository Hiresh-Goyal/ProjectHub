package com.projecthub.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdatePasswordRequest
{
    @NotBlank(message = "Current password is required")
    @Size(max = 50, message = "Current Password cannot exceed 50 characters")
    private String currentPass;

    @NotBlank(message = "New password is required")
    @Size(max = 50, message = "New Password cannot exceed 50 characters")
    private String newPass;
}
