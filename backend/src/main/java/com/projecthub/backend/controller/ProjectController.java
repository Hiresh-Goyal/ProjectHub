package com.projecthub.backend.controller;

import com.projecthub.backend.exception.ProjectNotFoundException;
import com.projecthub.backend.model.Project;
import com.projecthub.backend.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class ProjectController {

    private final ProjectService  projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping("/projects")
    public ResponseEntity<List<Project>> getProjects()
    {
        return ResponseEntity.ok(projectService.getProjects());
    }

    @GetMapping("/projects/{id}")
    public ResponseEntity<Project> getProject(@PathVariable long id) {
        return ResponseEntity.ok(projectService.getProjectbyID(id));
    }

    @PostMapping("/projects")
    public ResponseEntity<Project> addProject(@Valid @RequestBody Project project)
    {
        project = projectService.addProject(project);
        return ResponseEntity.status(HttpStatus.CREATED).body(project);
    }

    @PutMapping("/projects/{id}")
    public ResponseEntity<Project> updateProject(@Valid @RequestBody Project project, @PathVariable long id)
    {
        Project existingProject = projectService.getProjectbyID(id);
        existingProject.setName(project.getName());
        existingProject.setStatus(project.getStatus());
        projectService.updateProject(existingProject);
        return ResponseEntity.ok(existingProject);
    }

    @DeleteMapping("/projects/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable long id) {
        projectService.getProjectbyID(id);

        projectService.deleteProject(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/test-error")
    public String testError() {
        throw new RuntimeException("This is a test exception");
    }
}
