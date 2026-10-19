package com.projecthub.backend.mapper;

import com.projecthub.backend.dto.TaskAssignmentResponse;
import com.projecthub.backend.model.TaskAssignment;
import org.springframework.stereotype.Component;

@Component
public class TaskAssignmentMapper
{
    public TaskAssignmentResponse toTaskAssignmentResponse(TaskAssignment taskAssignment)
    {
        return new TaskAssignmentResponse(
                taskAssignment.getId(),
                taskAssignment.getTask().getId(),
                taskAssignment.getProjectMember().getId(),
                taskAssignment.getProjectMember().getUser().getId(),
                taskAssignment.getTask().getTitle(),
                taskAssignment.getProjectMember().getProject().getName(),
                taskAssignment.getProjectMember().getUser().getName(),
                taskAssignment.getCreatedAt()
        );
    }
}
