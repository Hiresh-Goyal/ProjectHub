package com.projecthub.backend.ai.mapper;

import com.projecthub.backend.ai.dto.ChatMessageResponse;
import com.projecthub.backend.ai.model.ChatMessage;
import org.springframework.stereotype.Component;

@Component
public class MessageMapper {

    public ChatMessageResponse toMessageResponse(ChatMessage message)
    {
        return new ChatMessageResponse(
                message.getId(),
                message.getRole(),
                message.getContent(),
                message.getCreatedAt()
        );
    }

}
