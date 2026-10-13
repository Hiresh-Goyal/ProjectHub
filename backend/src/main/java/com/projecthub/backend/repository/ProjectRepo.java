package com.projecthub.backend.repository;

import com.projecthub.backend.model.Project;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Repository
public class ProjectRepo
{
    private List<Project> projects = new ArrayList<>(Arrays.asList(
             new Project(1L,"Project 1", "In progress")
            ,new Project(2L,"Project 2", "In progress")
            ,new Project(3L,"Project 3", "In progress")));

    public List<Project> getProjects() {
        return projects;
    }

    public Project getProjectbyID(long id) {
        Project project = null;
        for (Project p : projects) {
            if (p.getId() != null && p.getId().equals(id))
                project = p;
        }
        return project;
    }

    public void addProject(Project project) {
        projects.add(project);
    }

    public void updateProject(Project project) {
        for(Project p : projects) {
            if (p.getId() != null && p.getId().equals(project.getId()))
            {
                p.setName(project.getName());
                p.setStatus(project.getStatus());
            }
        }
    }

    public void deleteProject(long id) {
        projects.removeIf(p -> (p.getId() != null && p.getId().equals(id)));
    }
}
