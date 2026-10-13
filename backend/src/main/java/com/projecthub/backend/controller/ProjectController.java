package com.projecthub.backend.controller;

import com.projecthub.backend.model.Project;
import com.projecthub.backend.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class ProjectController {

    ProjectService  projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping("/projects")
    public ResponseEntity<List<Project>> getProjects()
    {
        return ResponseEntity.ok(projectService.getProjects());
    }

    @GetMapping("/projects/{id}")
    public ResponseEntity<Project> getProject(@PathVariable int id)
    {
        Project project = projectService.getProjectbyID(id);
        if(project == null)
            return ResponseEntity.notFound().build();
        else
            return ResponseEntity.ok(project);
    }
}
