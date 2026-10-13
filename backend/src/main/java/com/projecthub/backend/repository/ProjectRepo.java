package com.projecthub.backend.repository;

import com.projecthub.backend.model.Project;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;

@Repository
public class ProjectRepo
{
    private List<Project> projects = Arrays.asList(
             new Project(1,"Project 1", "In progress")
            ,new Project(2,"Project 2", "In progress")
            ,new Project(3,"Project 3", "In progress"));

    public List<Project> getProjects() {
        return projects;
    }

    public Project getProjectbyID(int id) {
        Project project = null;
        for (Project p : projects) {
            if(p.getId() == id)
                project = p;
        }
        return project;
    }
}
