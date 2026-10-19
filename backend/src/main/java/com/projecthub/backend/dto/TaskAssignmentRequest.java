package com.projecthub.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class TaskAssignmentRequest
{
    @NotNull
    @Positive
    Long projectMemberId;
}
