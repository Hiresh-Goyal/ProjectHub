package com.projecthub.backend.service;

import com.projecthub.backend.dto.TaskRequest;
import com.projecthub.backend.dto.TaskResponse;
import com.projecthub.backend.exception.TaskNotFoundException;
import com.projecthub.backend.mapper.TaskMapper;
import com.projecthub.backend.model.Task;
import com.projecthub.backend.repository.TaskRepo;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TaskService
{
    private final TaskRepo taskRepo;
    private final ProjectService projectService;
    private final TaskMapper taskMapper;


    public TaskService(TaskRepo taskRepo, ProjectService projectService, TaskMapper taskMapper) {
        this.taskRepo = taskRepo;
        this.projectService = projectService;
        this.taskMapper = taskMapper;
    }

    public List<Task> getTasks() {
        return taskRepo.findAll();
    }

    public Task getTaskById(Long id)
    {
        return taskRepo.findById(id).orElseThrow(() ->
                new TaskNotFoundException("Task Not Found with ID : " + id));
    }

    public TaskResponse addTask(TaskRequest taskRequest)
    {
        Task task = taskMapper.toTask(taskRequest);
        task.setProject(projectService.getProjectbyId(taskRequest.getProjectId()));
        Task savedTask = taskRepo.save(task);

        return taskMapper.toTaskResponse(savedTask);
    }

    @Transactional
    public TaskResponse updateTask(TaskRequest taskRequest, long id) {
        Task existingTask = getTaskById(id);
        taskMapper.updateTask(taskRequest, existingTask);
        existingTask.setProject(projectService.getProjectbyId(taskRequest.getProjectId()));
        return taskMapper.toTaskResponse(existingTask);
    }

    public void deleteTask(long id)
    {
        taskRepo.deleteById(id);
    }
}
