package com.projecthub.backend.ai.controller;

import com.projecthub.backend.ai.dto.ChatMessageResponse;
import com.projecthub.backend.ai.dto.ConversationResponse;
import com.projecthub.backend.ai.dto.RenameConversationRequest;
import com.projecthub.backend.ai.service.ChatHistoryService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ai/conversations")
public class ChatHistoryController {
    private final ChatHistoryService chatHistoryService;

    public ChatHistoryController(ChatHistoryService chatHistoryService) {
        this.chatHistoryService = chatHistoryService;
    }

    @GetMapping
    public ResponseEntity<Page<ConversationResponse>> getMyConversations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if(page < 0 || size < 1) {
            throw new IllegalArgumentException("Page number and Page size must be greater than 0");
        } else if (size > 100) {
            throw new IllegalArgumentException("Page size must be between 1 - 100");
        }
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(chatHistoryService.getMyConversations(pageable));
    }

    @GetMapping("/{conversationId}/messages")
    public ResponseEntity<List<ChatMessageResponse>> getConversationMessages(@PathVariable Long conversationId)
    {
        return ResponseEntity.ok(chatHistoryService.getConversationMessages(conversationId));
    }

    @PatchMapping("/{conversationId}")
    public ResponseEntity<ConversationResponse> renameConversation(@PathVariable Long conversationId, @Valid @RequestBody RenameConversationRequest renameConversationRequest)
    {
        return ResponseEntity.ok(chatHistoryService.renameConversation(conversationId, renameConversationRequest.getNewTitle()));
    }

    @DeleteMapping("/{conversationId}")
    public ResponseEntity<Void> deleteConversation(@PathVariable Long conversationId)
    {
        chatHistoryService.deleteConversation(conversationId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
