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
import java.util.Objects;

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

        if(userService.isGlobalAdmin())
            return taskRepo.findAll();

        if(currentUser.getCompany() == null)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not belong to a company hence you have no tasks");
        if(userService.isCompanyOwner())
        {
            List<Project> projects = projectService.getProjects();

            return projects
                    .stream()
                    .map(project -> taskRepo.findByProjectId(project.getId()))
                    .flatMap(List::stream)
                    .toList();
        }

        List<Task> memberTasks =  projectMembers
                .stream()
                .map(ProjectMember::getProject)
                .map(project -> taskRepo.findByProjectId(project.getId()))
                .flatMap(List::stream)
                .filter(task -> projectService.sameCompany(task.getProject(), currentUser))
                .toList();

        List<Task> ownerTasks = taskRepo.findByProjectOwnerId(currentUser.getId()).stream()
                .filter(task -> projectService.sameCompany(task.getProject(), currentUser))
                .toList();

        List<Task> allTasks = new ArrayList<>(memberTasks);
        allTasks.addAll(ownerTasks);

        return  allTasks;
    }

    public Task getTaskById(Long id)
    {
        Task task = taskRepo.findById(id).orElseThrow(() ->
                new TaskNotFoundException("Task Not Found with ID : " + id));
        if(projectService.isOwnerOrMemberOrGlobalAdmin(task.getProject()))
            return task;
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to view this task");
    }

    public TaskResponse addTask(TaskRequest taskRequest)
    {
        Task task = taskMapper.toTask(taskRequest);
        if(projectService.isOwnerOrGlobalAdmin(taskRequest.getProjectId())) {
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
        Project project = projectService.getProjectById(taskRequest.getProjectId());

        if(!Objects.equals(existingTask.getProject().getCompany().getId(), project.getCompany().getId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tasks cannot be moved between companies");


        if(!(existingTask.getProject().getId().equals(taskRequest.getProjectId())) && (taskAssignmentRepo.existsByTaskId(id)))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot move task to another project as it has assigned people");

        if(projectService.isOwnerOrGlobalAdmin(taskRequest.getProjectId()) &&
                projectService.isOwnerOrGlobalAdmin(existingTask.getProject().getId()))
        {
            taskMapper.updateTask(taskRequest, existingTask);
            existingTask.setProject(project);
            return taskMapper.toTaskResponse(existingTask);
        }
        else
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to move task between projects");
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
        if(projectService.isOwnerOrGlobalAdmin(task.getProject().getId())) {
            taskAssignmentRepo.deleteByTaskId(id);
            taskRepo.deleteById(id);
        }
        else
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to delete this task");
    }
}
