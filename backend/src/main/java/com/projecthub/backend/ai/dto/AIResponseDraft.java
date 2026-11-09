package com.projecthub.backend.ai.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AIResponseDraft {
    @NotBlank
    private String message;
    @Positive
    @Nullable
    private Long projectId;

    @NotNull
    @Valid
    @Size(max = 5, message = "No of recommendations cannot exceed 5")
    private List<TaskRecommendationDraft> recommendations = new ArrayList<>();

    @NotNull
    @Valid
    @Size(max = 10, message = "No of task proposals cannot exceed 10")
    private List<TaskProposalDraft> taskProposals = new ArrayList<>();

}
