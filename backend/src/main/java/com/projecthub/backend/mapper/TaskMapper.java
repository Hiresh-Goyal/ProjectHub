package com.projecthub.backend.mapper;

import com.projecthub.backend.dto.TaskRequest;
import com.projecthub.backend.dto.TaskResponse;
import com.projecthub.backend.model.Task;
import org.springframework.stereotype.Component;

@Component
public class TaskMapper
{

    public Task toTask(TaskRequest taskRequest)
    {
        Task task = new Task();

        task.setTitle(taskRequest.getTitle());
        task.setDescription(taskRequest.getDescription());
        task.setStatus(taskRequest.getStatus());
        task.setPriority(taskRequest.getPriority());
        task.setDueDate(taskRequest.getDueDate());
        return task;
    }

    public TaskResponse toTaskResponse(Task task)
    {
        return new TaskResponse(task.getId(),task.getTitle(),task.getDescription(),
                task.getStatus(),task.getPriority(),task.getDueDate(),task.getProject().getId());
    }

    public void updateTask(TaskRequest taskRequest, Task existingTask)
    {
        existingTask.setTitle(taskRequest.getTitle());
        existingTask.setDescription(taskRequest.getDescription());
        existingTask.setStatus(taskRequest.getStatus());
        existingTask.setPriority(taskRequest.getPriority());
        existingTask.setDueDate(taskRequest.getDueDate());
    }
}
