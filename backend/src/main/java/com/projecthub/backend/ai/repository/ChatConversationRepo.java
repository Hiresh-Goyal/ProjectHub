package com.projecthub.backend.ai.repository;

import com.projecthub.backend.ai.model.ChatConversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatConversationRepo extends JpaRepository<ChatConversation, Long> {
    Page<ChatConversation> findAllByUser_IdOrderByUpdatedAtDesc(Long userId, Pageable pageable);

    Optional<ChatConversation> findByIdAndUser_Id(Long conversationId, Long userId);
}
