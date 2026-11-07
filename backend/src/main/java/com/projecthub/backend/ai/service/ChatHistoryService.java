package com.projecthub.backend.ai.service;

import com.projecthub.backend.ai.dto.ChatMessageResponse;
import com.projecthub.backend.ai.dto.ConversationResponse;
import com.projecthub.backend.ai.enums.ChatMessageRole;
import com.projecthub.backend.ai.mapper.ConversationMapper;
import com.projecthub.backend.ai.mapper.MessageMapper;
import com.projecthub.backend.ai.model.ChatConversation;
import com.projecthub.backend.ai.model.ChatMessage;
import com.projecthub.backend.ai.repository.ChatConversationRepo;
import com.projecthub.backend.ai.repository.ChatMessageRepo;
import com.projecthub.backend.model.User;
import com.projecthub.backend.service.UserService;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ChatHistoryService {

    private final UserService userService;
    private final ChatConversationRepo chatConversationRepo;
    private final ConversationMapper conversationMapper;
    private final ChatMessageRepo chatMessageRepo;
    private final MessageMapper messageMapper;
    private final EntityManager entityManager;

    public ChatHistoryService(UserService userService, ChatConversationRepo chatConversationRepo, ConversationMapper conversationMapper, ChatMessageRepo chatMessageRepo, MessageMapper messageMapper, EntityManager entityManager) {
        this.userService = userService;
        this.chatConversationRepo = chatConversationRepo;
        this.conversationMapper = conversationMapper;
        this.chatMessageRepo = chatMessageRepo;
        this.messageMapper = messageMapper;
        this.entityManager = entityManager;
    }

    public ConversationResponse createConversation(String initialMessage)
    {
        initialMessage = initialMessage.replaceAll("\\s+", " ").strip();
        User user = userService.getLoggedInUser();
        ChatConversation conversation = new ChatConversation();

        if(initialMessage.length() > 60)
        {
            int index = initialMessage.lastIndexOf(" ", 57);

            if (index == -1) {
                index = 57;
            }
            
            conversation.setTitle(initialMessage.substring(0, index).strip() + "...");
        }
        else
        {
            conversation.setTitle(initialMessage);
        }
        conversation.setUser(user);
        ChatConversation savedConvo = chatConversationRepo.save(conversation);
        return conversationMapper.toConversationResponse(savedConvo);

    }

    public Page<ConversationResponse> getMyConversations(Pageable pageable)
    {
        User user = userService.getLoggedInUser();
        Page<ChatConversation> conversations = chatConversationRepo.findAllByUser_IdOrderByUpdatedAtDesc(user.getId(), pageable);
        return conversations.map(conversationMapper::toConversationResponse);
    }

    public List<ChatMessageResponse> getConversationMessages(Long conversationId)
    {
        getConversationForCurrentUser(conversationId);

        List<ChatMessage> chatMessages = chatMessageRepo.findAllByConversation_IdOrderByCreatedAtAsc(conversationId);
        return chatMessages.stream().map(messageMapper::toMessageResponse).toList();
    }

    public ChatConversation getConversationForCurrentUser(Long conversationId)
    {
        User user = userService.getLoggedInUser();
        return chatConversationRepo.findByIdAndUser_Id(conversationId, user.getId()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "No conversation with given id was found to be associated with the logged in user"));
    }

    @Transactional
    public ChatMessageResponse saveMessage(Long conversationId, ChatMessageRole role, String content)
    {
        ChatMessage chatMessage = new ChatMessage();
        ChatConversation conversation = getConversationForCurrentUser(conversationId);
        chatMessage.setConversation(conversation);
        chatMessage.setRole(role);
        chatMessage.setContent(content);
        ChatMessage savedMessage = chatMessageRepo.save(chatMessage);
        conversation.setUpdatedAt(LocalDateTime.now());
        return messageMapper.toMessageResponse(savedMessage);
    }

    @Transactional
    public Long saveExchange(Long conversationId, String message, String aiResponse)
    {
        //creating convo if no exist
        if(conversationId == null)
        {
            ConversationResponse conversationResponse = createConversation(message);
            conversationId = conversationResponse.getConversationId();
        }

        //saving user message
        saveMessage(conversationId, ChatMessageRole.USER,message);

        //saving AI response
        saveMessage(conversationId, ChatMessageRole.ASSISTANT, aiResponse);

        return conversationId;
    }

    @Transactional
    public ConversationResponse renameConversation(Long conversationId, String newTitle)
    {
        ChatConversation conversation= getConversationForCurrentUser(conversationId);
        if(newTitle.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Conversation title cannot be blank");

        newTitle = newTitle.strip();
        conversation.setTitle(newTitle);
        entityManager.flush();
        return conversationMapper.toConversationResponse(conversation);
    }

    @Transactional
    public void deleteConversation(Long conversationId)
    {
        ChatConversation conversation = getConversationForCurrentUser(conversationId);
        chatMessageRepo.deleteAllByConversation_Id(conversationId);
        chatConversationRepo.delete(conversation);
    }
}

