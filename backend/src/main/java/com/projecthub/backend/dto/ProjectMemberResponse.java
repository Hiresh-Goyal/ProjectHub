package com.projecthub.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProjectMemberResponse {
    private Long id;
    private Long projectId;
    private Long userId;
    private String projectName;
    private String userName;
    private String userEmail;
    private LocalDateTime createdDate;

}
