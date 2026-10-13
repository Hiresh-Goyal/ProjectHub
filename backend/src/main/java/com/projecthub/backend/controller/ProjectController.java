package com.projecthub.backend.controller;

import com.projecthub.backend.model.Project;
import com.projecthub.backend.service.ProjectService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

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
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        else {
            projectService.addProject(project);
            return ResponseEntity.status(HttpStatus.CREATED).body(project);
        }
    }

    @PutMapping("/projects/{id}")
    public ResponseEntity<Project> updateProject(@RequestBody Project project, @PathVariable long id)
    {
        Project project1 = projectService.getProjectbyID(id);
        if(project1 != null)
        {
            project.setId(id);
            projectService.updateProject(project);
            return ResponseEntity.ok(project);
        }
        else
            return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/projects/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable long id)
    {
        Project project = projectService.getProjectbyID(id);
        if(project != null)
        {
            projectService.deleteProject(id);
            return ResponseEntity.noContent().build();
        }
        else
            return ResponseEntity.notFound().build();
    }
}
