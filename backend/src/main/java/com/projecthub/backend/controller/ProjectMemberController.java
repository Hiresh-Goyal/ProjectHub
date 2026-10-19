package com.projecthub.backend.controller;

import com.projecthub.backend.dto.ProjectMemberRequest;
import com.projecthub.backend.dto.ProjectMemberResponse;
import com.projecthub.backend.mapper.ProjectMemberMapper;
import com.projecthub.backend.model.ProjectMember;
import com.projecthub.backend.service.ProjectMemberService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class ProjectMemberController
{
    private final ProjectMemberService projectMemberService;
    private final ProjectMemberMapper  projectMemberMapper;

    public ProjectMemberController(ProjectMemberService projectMemberService, ProjectMemberMapper projectMemberMapper) {
        this.projectMemberService = projectMemberService;
        this.projectMemberMapper = projectMemberMapper;
    }

    @PostMapping("/projects/{projectId}/members")
    public ResponseEntity<ProjectMemberResponse> addMember(@Valid @RequestBody ProjectMemberRequest projectMemberRequest, @PathVariable Long projectId)
    {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectMemberService.addProjectMember(projectMemberRequest,projectId));
    }

    @GetMapping("/projects/{projectId}/members")
    public ResponseEntity<List<ProjectMemberResponse>> getProjectMembers(@PathVariable Long projectId)
    {
        List<ProjectMember> projectMembers = projectMemberService.getProjectMembers(projectId);
        List<ProjectMemberResponse> memberList = projectMembers.stream()
                .map(projectMemberMapper::toProjectMemberResponse)
                .toList();
        return ResponseEntity.ok(memberList);
    }

    @GetMapping("/projects/{projectId}/members/{userId}")
    public ResponseEntity<ProjectMemberResponse> getProjectMember(@PathVariable Long projectId, @PathVariable Long userId)
    {
        ProjectMember projectMember = projectMemberService.getProjectMember(projectId, userId);
        return ResponseEntity.ok(projectMemberMapper.toProjectMemberResponse(projectMember));
    }

    @DeleteMapping("/projects/{projectId}/members/{userId}")
    public ResponseEntity<Void> deleteProjectMember(@PathVariable Long projectId, @PathVariable Long userId)
    {
        projectMemberService.removeProjectMember(projectId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users/me/projects")
    public ResponseEntity<List<ProjectMemberResponse>> getUserProjects()
    {
        List<ProjectMember> memberProjects = projectMemberService.getUserProjects();
        List<ProjectMemberResponse> responseMemberProjects = memberProjects.stream()
                .map(projectMemberMapper::toProjectMemberResponse)
                .toList();
        return ResponseEntity.ok(responseMemberProjects);
    }
}
