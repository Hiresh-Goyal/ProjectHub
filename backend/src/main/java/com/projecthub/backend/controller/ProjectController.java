package com.projecthub.backend.controller;

import com.projecthub.backend.dto.CreateProjectRequest;
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
        List<ProjectResponse> responseList = projects.stream()
                .map(projectMapper::mapToResponse)
                .toList();

        return ResponseEntity.ok(responseList);
    }

    @GetMapping("/projects/{id}")
    public ResponseEntity<ProjectResponse> getProject(@PathVariable long id) {
        Project project = projectService.getProjectbyID(id);
        return ResponseEntity.ok(projectMapper.mapToResponse(project));
    }

    @PostMapping("/projects")
    public ResponseEntity<ProjectResponse> addProject(@Valid @RequestBody CreateProjectRequest projectDto)
    {
        Project project = projectMapper.toEntity(projectDto);

        Project savedProject = projectService.addProject(project);

        return ResponseEntity.status(HttpStatus.CREATED).body(projectMapper.mapToResponse(savedProject));
    }

    @PutMapping("/projects/{id}")
    public ResponseEntity<ProjectResponse> updateProject(@Valid @RequestBody CreateProjectRequest projectDto, @PathVariable long id)
    {
        Project existingProject = projectService.getProjectbyID(id);
        projectMapper.updateEntity(projectDto, existingProject);

        Project savedProject = projectService.updateProject(existingProject);

        return ResponseEntity.ok(projectMapper.mapToResponse(savedProject));
    }

    @DeleteMapping("/projects/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable long id) {
        projectService.getProjectbyID(id);

        projectService.deleteProject(id);
        return ResponseEntity.noContent().build();
    }

}
