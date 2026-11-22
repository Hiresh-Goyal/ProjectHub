package com.projecthub.backend.ai.service;

import com.projecthub.backend.ai.dto.TaskProposalDraft;
import com.projecthub.backend.ai.dto.TaskProposalResponse;
import com.projecthub.backend.ai.enums.TaskProposalStatus;
import com.projecthub.backend.service.ProjectService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class TaskProposalService {

    private final ProjectService projectService;

    public TaskProposalService(ProjectService projectService) {
        this.projectService = projectService;
    }

    public List<TaskProposalResponse> validateAndBuildProposals(
            Long projectId,
            List<TaskProposalDraft> taskProposals) {

        if((!taskProposals.isEmpty()) && (projectId == null))
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "AI returned an invalid task proposal response"
            );

        else if(taskProposals.isEmpty())
            return new ArrayList<>();

        projectService.getProjectById(projectId);

        List<TaskProposalResponse> taskProposalResponses = new ArrayList<>();

        for(TaskProposalDraft taskProposal : taskProposals)
        {
            TaskProposalResponse taskProposalResponse = new TaskProposalResponse(
                    UUID.randomUUID(),
                    taskProposal.getTitle(),
                    taskProposal.getDescription(),
                    taskProposal.getPriority(),
                    taskProposal.getDueDate(),
                    taskProposal.getRationale(),
                    TaskProposalStatus.PENDING,
                    null
            );
            taskProposalResponses.add(taskProposalResponse);
        }

        return taskProposalResponses;
    }
}
