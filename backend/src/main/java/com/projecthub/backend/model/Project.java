package com.projecthub.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Project
{
    private long id;
    private String name;
    private String status;
}
