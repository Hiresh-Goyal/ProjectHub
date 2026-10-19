package com.projecthub.backend.controller;

import com.projecthub.backend.dto.ProjectRequest;
import com.projecthub.backend.dto.ProjectResponse;
import com.projecthub.backend.mapper.ProjectMapper;
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
    private final ProjectMapper projectMapper;

    public ProjectController(ProjectService projectService, ProjectMapper projectMapper) {
        this.projectService = projectService;
        this.projectMapper = projectMapper;
    }

    @GetMapping("/projects")
    public ResponseEntity<List<ProjectResponse>> getProjects()
    {
        List<Project> projects = projectService.getProjects();
        return ResponseEntity.ok(projectMapper.toProjectResponses(projects));
    }

    @GetMapping("/projects/{id}")
    public ResponseEntity<ProjectResponse> getProject(@PathVariable long id) {
        Project project = projectService.getProjectById(id);
        return ResponseEntity.ok(projectMapper.toProjectResponse(project));
    }

    @PostMapping("/projects")
    public ResponseEntity<ProjectResponse> addProject(@Valid @RequestBody ProjectRequest projectRequest)
    {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.addProject(projectRequest));
    }

    @PutMapping("/projects/{id}")
    public ResponseEntity<ProjectResponse> updateProject(@Valid @RequestBody ProjectRequest projectRequest, @PathVariable long id)
    {
        return ResponseEntity.ok(projectService.updateProject(projectRequest, id));
    }

    @DeleteMapping("/projects/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable long id) {
        projectService.deleteProject(id);
        return ResponseEntity.noContent().build();
    }
}
