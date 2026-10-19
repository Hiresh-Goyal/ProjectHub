package com.projecthub.backend.controller;

import com.projecthub.backend.dto.TaskRequest;
import com.projecthub.backend.dto.TaskResponse;
import com.projecthub.backend.dto.TaskStatus;
import com.projecthub.backend.mapper.TaskMapper;
import com.projecthub.backend.model.Task;
import com.projecthub.backend.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class TaskController
{
    private final TaskService taskService;
    private final TaskMapper taskMapper;

    public TaskController(TaskService taskService, TaskMapper taskMapper) {
        this.taskService = taskService;
        this.taskMapper = taskMapper;
    }

    @GetMapping("/tasks")
    public ResponseEntity<List<TaskResponse>> getTasks()
    {
        List<Task> tasks = taskService.getTasks();
        return ResponseEntity.ok(taskMapper.toTaskResponses(tasks));
    }

    @GetMapping("/tasks/{id}")
    public ResponseEntity<TaskResponse> getTaskById(@PathVariable long id)
    {
        Task task = taskService.getTaskById(id);
        return ResponseEntity.ok(taskMapper.toTaskResponse(task));
    }

    @PostMapping("/tasks")
    public ResponseEntity<TaskResponse> addTask(@Valid @RequestBody TaskRequest taskRequest)
    {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.addTask(taskRequest));
    }

    @PutMapping("/tasks/{id}")
    public ResponseEntity<TaskResponse> updateTask(@Valid @RequestBody TaskRequest taskRequest, @PathVariable long id)
    {
        return ResponseEntity.ok(taskService.updateTask(taskRequest, id));
    }

    @DeleteMapping("/tasks/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable long id)
    {
        taskService.deleteTask(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PatchMapping("/tasks/{id}/status")
    public ResponseEntity<TaskResponse> updateStatus(@Valid @RequestBody TaskStatus taskStatus, @PathVariable long id)
    {
        return ResponseEntity.ok(taskService.updateStatus(taskStatus, id));
    }
}
