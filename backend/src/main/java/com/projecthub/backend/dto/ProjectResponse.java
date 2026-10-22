package com.projecthub.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProjectResponse
{
    private Long id;
    private String name;
    private String status;
    private Long companyId;
    private String companyName;
    private Long ownerId;
}
