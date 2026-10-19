package com.projecthub.backend.controller;

import com.projecthub.backend.dto.TaskAssignmentRequest;
import com.projecthub.backend.dto.TaskAssignmentResponse;
import com.projecthub.backend.mapper.TaskAssignmentMapper;
import com.projecthub.backend.model.TaskAssignment;
import com.projecthub.backend.service.TaskAssignmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class TaskAssignmentController {

    private final TaskAssignmentService taskAssignmentService;
    private final TaskAssignmentMapper taskAssignmentMapper;

    public TaskAssignmentController(TaskAssignmentService taskAssignmentService, TaskAssignmentMapper taskAssignmentMapper) {
        this.taskAssignmentService = taskAssignmentService;
        this.taskAssignmentMapper = taskAssignmentMapper;
    }

    @PostMapping("/tasks/{taskId}/assignees")
    public ResponseEntity<TaskAssignmentResponse> addTaskAssignment(@Valid @RequestBody TaskAssignmentRequest taskAssignmentRequest, @PathVariable Long taskId)
    {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskAssignmentService.addTaskAssignment(taskAssignmentRequest,taskId));
    }

    @GetMapping("/tasks/{taskId}/assignees")
    public ResponseEntity<List<TaskAssignmentResponse>> getTaskAssignees(@PathVariable Long taskId)
    {
        List<TaskAssignment> taskAssignments = taskAssignmentService.getTaskAssignees(taskId);
        List<TaskAssignmentResponse> taskAssignmentResponses = taskAssignments.stream()
                .map(taskAssignmentMapper::toTaskAssignmentResponse)
                .toList();
        return ResponseEntity.ok(taskAssignmentResponses);
    }

    @DeleteMapping("/task-assignments/{assignmentId}")
    public ResponseEntity<Void> deleteTaskAssignment(@PathVariable Long assignmentId)
    {
        taskAssignmentService.removeAssignedTask(assignmentId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users/me/assigned-tasks")
    public ResponseEntity<List<TaskAssignmentResponse>> getAssignedTasks()
    {
        List<TaskAssignment> taskAssignments = taskAssignmentService.getAssignedTasks();
        List<TaskAssignmentResponse> taskAssignmentResponses = taskAssignments.stream()
                .map(taskAssignmentMapper::toTaskAssignmentResponse)
                .toList();
        return ResponseEntity.ok(taskAssignmentResponses);
    }
}
