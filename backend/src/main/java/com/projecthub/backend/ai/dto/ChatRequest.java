package com.projecthub.backend.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChatRequest {

    @NotBlank
    @Size(max = 4000, message = "Your prompt cannot exceed 4000 char")
    private String message;

    @Positive
    private Long conversationId;

}
