package com.projecthub.backend.ai.dto;

import com.projecthub.backend.ai.enums.AIResponseType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessageStructuredData {
    Integer schemaVersion;
    AIResponseType responseType;
    @Nullable
    Long projectId;
    List<TaskRecommendationResponse> recommendations = new ArrayList<>();
    List<TaskProposalResponse> taskProposals = new ArrayList<>();
}
