package com.projecthub.backend.ai.repository;

import com.projecthub.backend.ai.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepo extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findAllByConversation_IdOrderByCreatedAtAsc(Long chatConversationId);
    void deleteAllByConversation_Id(Long conversationId);
}
