package com.projecthub.backend.ai.mapper;

import com.projecthub.backend.ai.dto.TaskProposalAcceptanceResponse;
import com.projecthub.backend.ai.dto.TaskProposalResponse;
import com.projecthub.backend.ai.enums.TaskProposalStatus;
import com.projecthub.backend.dto.TaskResponse;
import org.springframework.stereotype.Component;

@Component
public class TaskProposalAcceptanceMapper {

    public TaskProposalAcceptanceResponse toTaskProposalAcceptanceResponse(TaskProposalResponse proposal, TaskResponse savedTask)
    {
        return new TaskProposalAcceptanceResponse(
                proposal.getProposalId(),
                TaskProposalStatus.ACCEPTED,
                savedTask
        );
    }
}
