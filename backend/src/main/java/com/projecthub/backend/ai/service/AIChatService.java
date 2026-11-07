package com.projecthub.backend.ai.service;

import com.projecthub.backend.ai.dto.AIResponseDraft;
import com.projecthub.backend.ai.dto.ChatMessageResponse;
import com.projecthub.backend.ai.dto.ChatResponse;
import com.projecthub.backend.ai.dto.TaskRecommendationResponse;
import com.projecthub.backend.ai.enums.ChatMessageRole;
import com.projecthub.backend.ai.tools.ProjectRecommendationTools;
import com.projecthub.backend.ai.tools.ProjectTools;
import com.projecthub.backend.dto.TaskResponse;
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

    public AIChatService(ChatClient chatClient, ProjectTools projectTools, ProjectRecommendationTools projectRecommendationTools, ProjectRecommendationService projectRecommendationService, ChatHistoryService chatHistoryService, Validator validator) {
        this.chatClient = chatClient;
        this.projectTools = projectTools;
        this.projectRecommendationTools = projectRecommendationTools;
        this.projectRecommendationService = projectRecommendationService;
        this.chatHistoryService = chatHistoryService;
        this.validator = validator;
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
                AssistantMessage assistantMessage = new AssistantMessage(messageResponse.getContent());
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
        response.setRecommendations(validatedRecommendations);


        String assistantHistoryMessage = buildAssistantHistoryMessage(aiResponseDraft.getMessage(), validatedRecommendations);
        conversationId = chatHistoryService.saveExchange(conversationId, message, assistantHistoryMessage);

        response.setConversationId(conversationId);
        response.setResponse(aiResponseDraft.getMessage());
        return ResponseEntity.ok(response);
    }

    private String buildAssistantHistoryMessage(
            String message,
            List<TaskRecommendationResponse> recommendations
    ) {
        StringBuilder historyMessage = new StringBuilder(message);

        if (recommendations == null || recommendations.isEmpty()) {
            return historyMessage.toString();
        }

        historyMessage.append("\n\nRecommended Tasks:\n");

        for (int i = 0; i < recommendations.size(); i++) {

            TaskRecommendationResponse recommendation = recommendations.get(i);
            TaskResponse task = recommendation.getTask();

            historyMessage.append("\n")
                    .append(i + 1)
                    .append(". ")
                    .append(task.getTitle())
                    .append(" (Task ID: ")
                    .append(task.getId())
                    .append(")\n");

            historyMessage.append("   Priority: ")
                    .append(task.getPriority() != null ? task.getPriority() : "Not specified")
                    .append("\n");

            historyMessage.append("   Status: ")
                    .append(task.getStatus() != null ? task.getStatus() : "Not specified")
                    .append("\n");

            historyMessage.append("   Due Date: ")
                    .append(task.getDueDate() != null ? task.getDueDate() : "No due date")
                    .append("\n");

            historyMessage.append("   Reason: ")
                    .append(recommendation.getReason())
                    .append("\n");
        }

        return historyMessage.toString().stripTrailing();
    }
}
