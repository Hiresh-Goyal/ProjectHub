package com.projecthub.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class TransferProjectOwnershipRequest {
    @Positive
    @NotNull
    private Long userId;
}
