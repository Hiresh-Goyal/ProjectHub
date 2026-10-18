package com.projecthub.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateUserRequest
{
    @NotBlank(message = "User name is required")
    @Size(max = 100, message = "Name cannot be longer than 100 characters")
    private String name;

    @NotBlank(message = "User email is required")
    @Size(max = 200, message = "Email cannot exceed 200 characters")
    @Email
    private String email;

    @NotBlank(message = "User password is required")
    @Size(max = 50, message = "User Password cannot exceed 50 characters")
    private String password;
}
