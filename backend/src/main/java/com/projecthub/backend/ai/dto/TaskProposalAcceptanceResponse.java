package com.projecthub.backend.ai.dto;

import com.projecthub.backend.ai.enums.TaskProposalStatus;
import com.projecthub.backend.dto.TaskResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskProposalAcceptanceResponse {
    private UUID proposalId;
    private TaskProposalStatus status;
    private TaskResponse response;
}
