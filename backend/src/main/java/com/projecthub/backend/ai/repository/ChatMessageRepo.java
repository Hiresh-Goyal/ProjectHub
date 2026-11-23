package com.projecthub.backend.ai.repository;

import com.projecthub.backend.ai.model.ChatMessage;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatMessageRepo extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findAllByConversation_IdOrderByCreatedAtAsc(Long chatConversationId);
    void deleteAllByConversation_Id(Long conversationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("Select m from ChatMessage m where m.id = :id")
    Optional<ChatMessage> findByIdForUpdate(@Param("id") Long id);
}
