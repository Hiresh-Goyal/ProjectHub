package com.projecthub.backend.ai.service;

import com.projecthub.backend.ai.dto.*;
import com.projecthub.backend.ai.enums.AIResponseType;
import com.projecthub.backend.ai.enums.ChatMessageRole;
import com.projecthub.backend.ai.tools.ProjectRecommendationTools;
import com.projecthub.backend.ai.tools.ProjectTools;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class AIChatService {

    private final ChatClient chatClient;
    private final ProjectTools projectTools;
    private final ProjectRecommendationTools projectRecommendationTools;
    private final ProjectRecommendationService projectRecommendationService;
    private final ChatHistoryService chatHistoryService;
    private final Validator validator;
    private final TaskProposalService taskProposalService;
    private final ObjectMapper objectMapper;

    public AIChatService(ChatClient chatClient, ProjectTools projectTools, ProjectRecommendationTools projectRecommendationTools, ProjectRecommendationService projectRecommendationService, ChatHistoryService chatHistoryService, Validator validator, TaskProposalService taskProposalService, ObjectMapper objectMapper) {
        this.chatClient = chatClient;
        this.projectTools = projectTools;
        this.projectRecommendationTools = projectRecommendationTools;
        this.projectRecommendationService = projectRecommendationService;
        this.chatHistoryService = chatHistoryService;
        this.validator = validator;
        this.taskProposalService = taskProposalService;
        this.objectMapper = objectMapper;
    }

    public ResponseEntity<ChatResponse> generatedResponse(String message, Long conversationId)
    {
        List<ChatMessageResponse> messageResponseList;
        ChatResponse response = new ChatResponse();
        if(conversationId == null)
        {
            messageResponseList = new ArrayList<>();
        }
        else
        {
            //getting history of messages
            messageResponseList = new ArrayList<>(chatHistoryService.getConversationMessages(conversationId));
        }

        //converting to spring ai messages
        List<Message> springAiMessages = new ArrayList<>();

        for(ChatMessageResponse messageResponse : messageResponseList)
        {
            if(messageResponse.getRole() == ChatMessageRole.USER)
            {
                UserMessage userMessage = new UserMessage(messageResponse.getContent());
                springAiMessages.add(userMessage);
            }
            else if (messageResponse.getRole() == ChatMessageRole.ASSISTANT)
            {
                String content = messageResponse.getContent();

                if (messageResponse.getStructuredData() != null)
                {
                    String structuredJson = objectMapper.writeValueAsString(
                            messageResponse.getStructuredData()
                    );

                    content += "\n\nHistorical structured result "
                            + "(for context only; may not reflect current project state). "
                            + "Use ProjectHub tools to verify current information:\n"
                            + structuredJson;
                }

                AssistantMessage assistantMessage = new AssistantMessage(content);
                springAiMessages.add(assistantMessage);
            }
        }

        springAiMessages.add(new UserMessage(message));

        //calling model and getting back AIResponseDraft entity
        AIResponseDraft aiResponseDraft = chatClient.prompt()
                .messages(springAiMessages)
                .tools(projectTools, projectRecommendationTools)
                .call()
                .entity(AIResponseDraft.class, spec -> spec.validateSchema());

        //VALIDATION CHECKS
        if(aiResponseDraft == null)
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Invalid Model Response");

        Set<ConstraintViolation<AIResponseDraft>> violations = validator.validate(aiResponseDraft);

        if(!violations.isEmpty()) {
            violations.forEach(violation ->
                    log.error("Field: {}, Invalid value: {}, Message: {}",
                            violation.getPropertyPath(),
                            violation.getInvalidValue(),
                            violation.getMessage())
            );
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Invalid Model Response");
        } else if ((!aiResponseDraft.getRecommendations().isEmpty()) && (aiResponseDraft.getProjectId() == null)) {
            log.error("Project id is null when recommendations are present");
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Invalid Model Response");
        }

        List<TaskRecommendationResponse> validatedRecommendations = projectRecommendationService
                .validateAndBuildRecommendations(
                        aiResponseDraft.getProjectId(),
                        aiResponseDraft.getRecommendations()
                );

        List<TaskProposalResponse> validatedTaskProposals = taskProposalService
                .validateAndBuildProposals(
                        aiResponseDraft.getProjectId(),
                        aiResponseDraft.getTaskProposals()
                );
        response.setRecommendations(validatedRecommendations);
        response.setTaskProposals(validatedTaskProposals);

        ChatMessageStructuredData structuredData = new ChatMessageStructuredData();

        structuredData.setSchemaVersion(Integer.parseInt(System.getenv("CURRENT_SCHEMA_VERSION")));

        structuredData.setResponseType(
                determineResponseType(validatedRecommendations, validatedTaskProposals)
        );

        structuredData.setProjectId(
                (validatedRecommendations.isEmpty() && validatedTaskProposals.isEmpty())
                        ? null
                        : aiResponseDraft.getProjectId()
        );

        structuredData.setRecommendations(validatedRecommendations);
        structuredData.setTaskProposals(validatedTaskProposals);

        ChatExchangeSaveResult saveResult = chatHistoryService.saveExchange(conversationId, message, aiResponseDraft.getMessage(), structuredData);

        response.setAssistantMessageId(saveResult.getAssistantMessageId());
        response.setProjectId(aiResponseDraft.getProjectId());
        response.setConversationId(saveResult.getConversationId());
        response.setResponse(aiResponseDraft.getMessage());
        return ResponseEntity.ok(response);
    }

    private AIResponseType determineResponseType(
            List<TaskRecommendationResponse> recommendations,
            List<TaskProposalResponse> taskProposals
    ) {
        if (!recommendations.isEmpty() && !taskProposals.isEmpty()) {
            return AIResponseType.MIXED;
        }

        if (!recommendations.isEmpty()) {
            return AIResponseType.RECOMMENDATION;
        }

        if (!taskProposals.isEmpty()) {
            return AIResponseType.TASK_PLANNING;
        }

        return AIResponseType.GENERAL;
    }
}
