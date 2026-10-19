package com.projecthub.backend.service;

import com.projecthub.backend.dto.TaskAssignmentRequest;
import com.projecthub.backend.dto.TaskAssignmentResponse;
import com.projecthub.backend.mapper.TaskAssignmentMapper;
import com.projecthub.backend.model.ProjectMember;
import com.projecthub.backend.model.Task;
import com.projecthub.backend.model.TaskAssignment;
import com.projecthub.backend.repository.TaskAssignmentRepo;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class TaskAssignmentService {
    private final TaskAssignmentRepo taskAssignmentRepo;
    private final TaskAssignmentMapper taskAssignmentMapper;
    private final TaskService taskService;
    private final ProjectMemberService projectMemberService;
    private final ProjectService  projectService;

    public TaskAssignmentService(TaskAssignmentRepo taskAssignmentRepo, TaskAssignmentMapper taskAssignmentMapper, ProjectMemberService projectMemberService, ProjectService projectService, TaskService taskService) {
        this.taskAssignmentRepo = taskAssignmentRepo;
        this.taskAssignmentMapper = taskAssignmentMapper;
        this.projectMemberService = projectMemberService;
        this.projectService = projectService;
        this.taskService = taskService;
    }

    public TaskAssignmentResponse addTaskAssignment(TaskAssignmentRequest taskAssignmentRequest, Long taskId) {
        ProjectMember projectMember = projectMemberService.getMemberByProjectMemberId(taskAssignmentRequest.getProjectMemberId());
        Task task = taskService.getTaskById(taskId);

        if((projectService.isProjectOwnerOrAdmin(projectMember.getProject().getId())) && (task.getProject().getId().equals(projectMember.getProject().getId())))
        {
            TaskAssignment taskAssignment = new TaskAssignment();
            taskAssignment.setProjectMember(projectMember);
            taskAssignment.setTask(task);
            if(taskAssignmentRepo.existsByTaskIdAndProjectMemberId(taskId, projectMember.getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,"The task is already assigned to this user");
            }
            TaskAssignment savedTaskAssignment = taskAssignmentRepo.save(taskAssignment);
            return taskAssignmentMapper.toTaskAssignmentResponse(savedTaskAssignment);
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to assign tasks");
    }

    public List<TaskAssignment> getTaskAssignees(Long taskId)
    {
        //checks if admin,owner,member
        taskService.getTaskById(taskId);

        return taskAssignmentRepo.findAllByTaskId(taskId);
    }

    public List<TaskAssignment> getAssignedTasks()
    {
        List<ProjectMember> userProjects= projectMemberService.getUserProjects();

        return userProjects.stream()
                .map(projectMember -> taskAssignmentRepo.findAllByProjectMemberId(projectMember.getId()))
                .flatMap(List::stream)
                .toList();
    }

    private TaskAssignment getTaskAssignment(Long taskAssignmentId){
        return taskAssignmentRepo.findById(taskAssignmentId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND,"The task assignment could not be found")
        );
    }

    public void removeAssignedTask(Long taskAssignmentId)
    {
        TaskAssignment taskAssignment = getTaskAssignment(taskAssignmentId);
        if(projectService.isProjectOwnerOrAdmin(taskAssignment.getTask().getProject().getId())) {
            taskAssignmentRepo.delete(taskAssignment);
        }
        else
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to delete task assignment");
    }
}
