package com.projecthub.backend.ai.service;

import com.projecthub.backend.ai.dto.*;
import com.projecthub.backend.ai.enums.ChatMessageRole;
import com.projecthub.backend.ai.enums.TaskProposalStatus;
import com.projecthub.backend.ai.mapper.ConversationMapper;
import com.projecthub.backend.ai.mapper.MessageMapper;
import com.projecthub.backend.ai.mapper.TaskProposalAcceptanceMapper;
import com.projecthub.backend.ai.model.ChatConversation;
import com.projecthub.backend.ai.model.ChatMessage;
import com.projecthub.backend.ai.repository.ChatConversationRepo;
import com.projecthub.backend.ai.repository.ChatMessageRepo;
import com.projecthub.backend.dto.TaskRequest;
import com.projecthub.backend.dto.TaskResponse;
import com.projecthub.backend.enums.TaskStatus;
import com.projecthub.backend.mapper.TaskMapper;
import com.projecthub.backend.model.User;
import com.projecthub.backend.service.TaskService;
import com.projecthub.backend.service.UserService;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class ChatHistoryService {

    private final UserService userService;
    private final ChatConversationRepo chatConversationRepo;
    private final ConversationMapper conversationMapper;
    private final ChatMessageRepo chatMessageRepo;
    private final MessageMapper messageMapper;
    private final EntityManager entityManager;
    private final TaskService taskService;
    private final TaskMapper taskMapper;
    private final TaskProposalAcceptanceMapper taskProposalAcceptanceMapper;

    public ChatHistoryService(UserService userService, ChatConversationRepo chatConversationRepo, ConversationMapper conversationMapper, ChatMessageRepo chatMessageRepo, MessageMapper messageMapper, EntityManager entityManager, TaskService taskService, TaskMapper taskMapper, TaskProposalAcceptanceMapper taskProposalAcceptanceMapper) {
        this.userService = userService;
        this.chatConversationRepo = chatConversationRepo;
        this.conversationMapper = conversationMapper;
        this.chatMessageRepo = chatMessageRepo;
        this.messageMapper = messageMapper;
        this.entityManager = entityManager;
        this.taskService = taskService;
        this.taskMapper = taskMapper;
        this.taskProposalAcceptanceMapper = taskProposalAcceptanceMapper;
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
    public ChatMessageResponse saveMessage(Long conversationId,
                                           ChatMessageRole role,
                                           String content,
                                           ChatMessageStructuredData structuredData)
    {
        ChatMessage chatMessage = new ChatMessage();
        ChatConversation conversation = getConversationForCurrentUser(conversationId);
        chatMessage.setConversation(conversation);
        chatMessage.setRole(role);
        chatMessage.setContent(content);
        chatMessage.setStructuredData(structuredData);
        ChatMessage savedMessage = chatMessageRepo.save(chatMessage);
        conversation.setUpdatedAt(LocalDateTime.now());
        return messageMapper.toMessageResponse(savedMessage);
    }

    @Transactional
    public ChatExchangeSaveResult saveExchange(Long conversationId,
                             String message,
                             String aiResponse,
                             ChatMessageStructuredData structuredData)
    {
        //creating convo if no exist
        if(conversationId == null)
        {
            ConversationResponse conversationResponse = createConversation(message);
            conversationId = conversationResponse.getConversationId();
        }

        //saving user message
        saveMessage(conversationId, ChatMessageRole.USER, message, null);

        //saving AI response
        ChatMessageResponse messageResponse = saveMessage(conversationId, ChatMessageRole.ASSISTANT, aiResponse, structuredData);

        ChatExchangeSaveResult saveResult = new ChatExchangeSaveResult(
                conversationId,
                messageResponse.getMessageId()
        );

        return saveResult;
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

    @Transactional
    public TaskProposalAcceptanceResponse createAcceptedTask(Long conversationId, Long messageId, UUID proposalId) {
        getConversationForCurrentUser(conversationId);
        ChatMessage message = chatMessageRepo.findByIdForUpdate(messageId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "No message with the provided id was found"));

        if(!Objects.equals(message.getConversation().getId(), conversationId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This message does not belong the provided conversation ID");

        if(message.getRole() != ChatMessageRole.ASSISTANT)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The messageId belongs to a user prompt");

        ChatMessageStructuredData structuredData = message.getStructuredData();
        if(structuredData!= null)
        {
            if(structuredData.getSchemaVersion() == Integer.parseInt(System.getenv("CURRENT_SCHEMA_VERSION")))
            {
                List<TaskProposalResponse> proposals = structuredData.getTaskProposals();
                if (proposals != null) {
                    for (TaskProposalResponse proposal : proposals) {
                        if (proposal.getProposalId().equals(proposalId)) {
                            if(!(proposal.getStatus() == TaskProposalStatus.PENDING))
                                return taskProposalAcceptanceMapper.toTaskProposalAcceptanceResponse(proposal, taskMapper.toTaskResponse(taskService.getTaskById(proposal.getCreatedTaskId())));

                            TaskRequest taskRequest = new TaskRequest(
                                    proposal.getTitle(),
                                    proposal.getDescription(),
                                    TaskStatus.TODO,
                                    proposal.getPriority(),
                                    proposal.getDueDate(),
                                    structuredData.getProjectId()
                            );

                            TaskResponse savedTask = taskService.addTask(taskRequest);


                            ChatMessageStructuredData newStructuredData = new ChatMessageStructuredData();

                            newStructuredData.setSchemaVersion(structuredData.getSchemaVersion());
                            newStructuredData.setResponseType(structuredData.getResponseType());
                            newStructuredData.setProjectId(structuredData.getProjectId());
                            newStructuredData.setRecommendations(structuredData.getRecommendations());

                            List<TaskProposalResponse> newProposals = new ArrayList<>();
                            for(TaskProposalResponse newProposal : proposals)
                            {
                                TaskProposalResponse newProposalResponse = new TaskProposalResponse(
                                        newProposal.getProposalId(),
                                        newProposal.getTitle(),
                                        newProposal.getDescription(),
                                        newProposal.getPriority(),
                                        newProposal.getDueDate(),
                                        newProposal.getRationale(),
                                        newProposal.getStatus(),
                                        newProposal.getCreatedTaskId()
                                );

                                if (newProposal.getProposalId().equals(proposalId))
                                {
                                    newProposalResponse.setStatus(TaskProposalStatus.ACCEPTED);
                                    newProposalResponse.setCreatedTaskId(savedTask.getId());
                                }
                                newProposals.add(newProposalResponse);
                            }

                            newStructuredData.setTaskProposals(newProposals);

                            message.setStructuredData(newStructuredData);
                            return taskProposalAcceptanceMapper.toTaskProposalAcceptanceResponse(proposal, savedTask);
                        }
                    }
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No proposal with given proposalId was found");
                }
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "The structured data contained no proposals");
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The structured data belongs to an older schema version");
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Message contains no structured data");
    }
}

