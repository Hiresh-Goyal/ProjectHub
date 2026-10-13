package com.projecthub.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Project
{
    private Long id;
    private String name;
    private String status;
}
