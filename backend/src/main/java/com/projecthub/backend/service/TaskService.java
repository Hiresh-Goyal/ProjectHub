package com.projecthub.backend.service;

import com.projecthub.backend.dto.TaskRequest;
import com.projecthub.backend.dto.TaskResponse;
import com.projecthub.backend.dto.TaskStatus;
import com.projecthub.backend.exception.TaskNotFoundException;
import com.projecthub.backend.mapper.TaskMapper;
import com.projecthub.backend.model.Project;
import com.projecthub.backend.model.ProjectMember;
import com.projecthub.backend.model.Task;
import com.projecthub.backend.model.User;
import com.projecthub.backend.repository.ProjectMemberRepo;
import com.projecthub.backend.repository.TaskAssignmentRepo;
import com.projecthub.backend.repository.TaskRepo;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
public class TaskService
{
    private final TaskRepo taskRepo;
    private final ProjectService projectService;
    private final TaskMapper taskMapper;
    private final UserService userService;
    private final ProjectMemberRepo projectMemberRepo;
    private final TaskAssignmentRepo taskAssignmentRepo;


    public TaskService(TaskRepo taskRepo, ProjectService projectService, TaskMapper taskMapper, UserService userService, ProjectMemberRepo projectMemberRepo, TaskAssignmentRepo taskAssignmentRepo) {
        this.taskRepo = taskRepo;
        this.projectService = projectService;
        this.taskMapper = taskMapper;
        this.userService = userService;
        this.projectMemberRepo = projectMemberRepo;
        this.taskAssignmentRepo = taskAssignmentRepo;
    }

    public List<Task> getTasks() {
        User currentUser = userService.getLoggedInUser();
        List<ProjectMember> projectMembers = projectMemberRepo.findAllByUserId(currentUser.getId());

        if(userService.isAdmin())
            return taskRepo.findAll();

        List<Task> memberTasks =  projectMembers
                .stream()
                .map(ProjectMember::getProject)
                .map(project -> taskRepo.findByProjectId(project.getId()))
                .flatMap(List::stream)
                .toList();

        List<Task> ownerTasks = taskRepo.findByProjectOwnerId(currentUser.getId());

        List<Task> allTasks = new ArrayList<>(memberTasks);
        allTasks.addAll(ownerTasks);

        return  allTasks;
    }

    public Task getTaskById(Long id)
    {
        User currentUser =  userService.getLoggedInUser();
        Task task = taskRepo.findById(id).orElseThrow(() ->
                new TaskNotFoundException("Task Not Found with ID : " + id));
        if((projectService.isProjectOwnerOrAdmin(task.getProject().getId())) ||
                (projectMemberRepo.existsByProjectIdAndUserId(task.getProject().getId(), currentUser.getId())))
            return task;
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to view this task");
    }

    public TaskResponse addTask(TaskRequest taskRequest)
    {
        Task task = taskMapper.toTask(taskRequest);
        if(projectService.isProjectOwnerOrAdmin(taskRequest.getProjectId())) {
            Project project = projectService.getProjectById(taskRequest.getProjectId());
            task.setProject(project);
            Task savedTask = taskRepo.save(task);
            return taskMapper.toTaskResponse(savedTask);
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to access this project");
    }

    @Transactional
    public TaskResponse updateTask(TaskRequest taskRequest, long id) {
        Task existingTask = getTaskById(id);
        if(projectService.isProjectOwnerOrAdmin(existingTask.getProject().getId()) &&
                (projectService.isProjectOwnerOrAdmin(taskRequest.getProjectId()))) {

            if(!(existingTask.getProject().getId().equals(taskRequest.getProjectId())) && (taskAssignmentRepo.existsByTaskId(id)))
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot move task to another project as it has assigned people");

            taskMapper.updateTask(taskRequest, existingTask);
            existingTask.setProject(projectService.getProjectById(taskRequest.getProjectId()));
            return taskMapper.toTaskResponse(existingTask);
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to update this task");
    }

    @Transactional
    public TaskResponse updateStatus(TaskStatus taskStatus, long id){
        Task existingTask = getTaskById(id);
        existingTask.setStatus(taskStatus.getStatus());
        return taskMapper.toTaskResponse(existingTask);
    }

    @Transactional
    public void deleteTask(long id)
    {
        Task task = getTaskById(id);
        if(projectService.isProjectOwnerOrAdmin(task.getProject().getId())) {
            taskAssignmentRepo.deleteByTaskId(id);
            taskRepo.deleteById(id);
        }
        else
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to delete this task");
    }
}
