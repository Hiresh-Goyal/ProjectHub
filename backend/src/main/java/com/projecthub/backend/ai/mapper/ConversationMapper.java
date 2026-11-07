package com.projecthub.backend.ai.mapper;

import com.projecthub.backend.ai.dto.ConversationResponse;
import com.projecthub.backend.ai.model.ChatConversation;
import org.springframework.stereotype.Component;

@Component
public class ConversationMapper {

    public ConversationResponse toConversationResponse(ChatConversation conversation) {
        return new ConversationResponse(conversation.getId(),
                conversation.getTitle(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt());
    }


}
