package com.projecthub.backend.ai.dto;

import com.projecthub.backend.ai.enums.ChatMessageRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessageResponse {
    private Long messageId;
    private ChatMessageRole role;
    private String content;
    private LocalDateTime createdAt;
}
