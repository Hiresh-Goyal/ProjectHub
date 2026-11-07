package com.projecthub.backend.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RenameConversationRequest {
    @NotBlank
    @Size(max = 60, message = "Conversation title length cannot exceed 60 characters")
    private String newTitle;
}
