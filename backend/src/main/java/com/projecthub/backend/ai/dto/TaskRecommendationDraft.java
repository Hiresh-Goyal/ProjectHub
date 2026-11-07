package com.projecthub.backend.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskRecommendationDraft {
    @NotNull
    @Positive
    private Long taskId;
    @NotBlank
    @Size(max = 500, message = "Reason length cannot exceed 500")
    private String reason;
}
