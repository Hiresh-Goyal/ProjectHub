package com.projecthub.backend.service;

import com.projecthub.backend.dto.TaskRequest;
import com.projecthub.backend.dto.TaskResponse;
import com.projecthub.backend.exception.TaskNotFoundException;
import com.projecthub.backend.mapper.TaskMapper;
import com.projecthub.backend.model.Project;
import com.projecthub.backend.model.Task;
import com.projecthub.backend.model.User;
import com.projecthub.backend.repository.TaskRepo;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class TaskService
{
    private final TaskRepo taskRepo;
    private final ProjectService projectService;
    private final TaskMapper taskMapper;
    private final UserService userService;


    public TaskService(TaskRepo taskRepo, ProjectService projectService, TaskMapper taskMapper, UserService userService) {
        this.taskRepo = taskRepo;
        this.projectService = projectService;
        this.taskMapper = taskMapper;
        this.userService = userService;
    }

    public List<Task> getTasks() {
        User user = userService.getLoggedInUser();
        return taskRepo.findByProjectOwnerId(user.getId());
    }

    public Task getTaskById(Long id)
    {
        User currentUser =  userService.getLoggedInUser();
        Task task = taskRepo.findById(id).orElseThrow(() ->
                new TaskNotFoundException("Task Not Found with ID : " + id));
        if(task.getProject().getOwner().getId().equals(currentUser.getId()) ||
                (currentUser.getRoles().stream().anyMatch(role -> role.getName().equals("ROLE_ADMIN"))))
            return task;
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to view this task");
    }

    public TaskResponse addTask(TaskRequest taskRequest)
    {
        Task task = taskMapper.toTask(taskRequest);
        Project project = projectService.getProjectById(taskRequest.getProjectId());
        task.setProject(project);
        Task savedTask = taskRepo.save(task);
        return taskMapper.toTaskResponse(savedTask);
    }

    @Transactional
    public TaskResponse updateTask(TaskRequest taskRequest, long id) {
        Task existingTask = getTaskById(id);
        taskMapper.updateTask(taskRequest, existingTask);
        existingTask.setProject(projectService.getProjectById(taskRequest.getProjectId()));
        return taskMapper.toTaskResponse(existingTask);
    }

    public void deleteTask(long id)
    {
        taskRepo.deleteById(id);
    }
}
