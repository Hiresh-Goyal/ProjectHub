package com.projecthub.backend.controller;

import com.projecthub.backend.model.Project;
import com.projecthub.backend.service.ProjectService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<Project> getProject(@PathVariable long id)
    {
        Project project = projectService.getProjectbyID(id);
        if(project == null)
            return ResponseEntity.notFound().build();
        else
            return ResponseEntity.ok(project);
    }

    @PostMapping("/projects")
    public ResponseEntity<Project> addProject(@RequestBody Project project)
    {
        Project project1 = projectService.getProjectbyID(project.getId());
        if(project1 != null)
            return ResponseEntity.badRequest().build();
        else {
            projectService.addProject(project);
            return ResponseEntity.ok(project);
        }
    }
}
