package com.projecthub.backend.mapper;

import com.projecthub.backend.dto.ProjectMemberResponse;
import com.projecthub.backend.model.ProjectMember;
import org.springframework.stereotype.Component;

@Component
public class ProjectMemberMapper {
    public ProjectMemberResponse toProjectMemberResponse(ProjectMember projectMember) {
        return new ProjectMemberResponse(
                projectMember.getId(),
                projectMember.getProject().getId(),
                projectMember.getUser().getId(),
                projectMember.getProject().getName(),
                projectMember.getUser().getName(),
                projectMember.getUser().getEmail(),
                projectMember.getCreatedAt()
        );
    }
}
