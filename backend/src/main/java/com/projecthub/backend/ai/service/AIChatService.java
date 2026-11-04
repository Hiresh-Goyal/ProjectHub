package com.projecthub.backend.ai.service;

import com.projecthub.backend.ai.dto.ChatResponse;
import com.projecthub.backend.ai.tools.ProjectTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class AIChatService {

    private final ChatClient chatClient;
    private final ProjectTools projectTools;

    public AIChatService(ChatClient chatClient, ProjectTools projectTools) {
        this.chatClient = chatClient;
        this.projectTools = projectTools;
    }

    public ResponseEntity<ChatResponse> generatedResponse(String message)
    {
        ChatResponse response = new ChatResponse(chatClient
                .prompt()
                .user(message)
                .tools(projectTools)
                .call()
                .content());
        return ResponseEntity.ok(response);
    }
}
