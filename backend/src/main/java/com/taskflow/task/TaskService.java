package com.taskflow.task;

import com.taskflow.common.Exceptions.BadRequestException;
import com.taskflow.common.Exceptions.ConflictException;
import com.taskflow.common.Exceptions.ForbiddenException;
import com.taskflow.common.Exceptions.NotFoundException;
import com.taskflow.notification.NotificationService;
import com.taskflow.project.Project;
import com.taskflow.project.ProjectRepository;
import com.taskflow.task.TaskDtos.CreateTaskRequest;
import com.taskflow.task.TaskDtos.TaskDto;
import com.taskflow.task.TaskDtos.UpdateTaskRequest;
import com.taskflow.team.TeamAccess;
import com.taskflow.team.TeamMember;
import com.taskflow.team.TeamMemberRepository;
import com.taskflow.team.TeamRole;
import com.taskflow.user.User;
import com.taskflow.user.UserRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TaskService {

    private final TaskRepository tasks;
    private final ProjectRepository projects;
    private final UserRepository users;
    private final TeamMemberRepository members;
    private final TeamAccess access;
    private final NotificationService notifications;

    public TaskService(
            TaskRepository tasks,
            ProjectRepository projects,
            UserRepository users,
            TeamMemberRepository members,
            TeamAccess access,
            NotificationService notifications) {
        this.tasks = tasks;
        this.projects = projects;
        this.users = users;
        this.members = members;
        this.access = access;
        this.notifications = notifications;
    }

    @Transactional(readOnly = true)
    public List<TaskDto> list(Long projectId, Long userId, TaskStatus status, Long assigneeId) {
        Project project = loadProject(projectId, userId);
        Specification<Task> spec = TaskSpecifications.inProject(project.getId());
        if (status != null) {
            spec = spec.and(TaskSpecifications.hasStatus(status));
        }
        if (assigneeId != null) {
            spec = spec.and(TaskSpecifications.assignedTo(assigneeId));
        }
        return tasks.findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .map(TaskDto::from)
                .toList();
    }

    public TaskDto create(Long projectId, Long userId, CreateTaskRequest request) {
        Project project = loadProject(projectId, userId);
        TaskPriority priority = request.priority() != null ? request.priority() : TaskPriority.MEDIUM;
        Task task = new Task(
                project, request.title().trim(), request.description(), priority, users.getReferenceById(userId));
        task.setDueDate(request.dueDate());
        if (request.assigneeId() != null) {
            assign(task, request.assigneeId(), userId);
        }
        return TaskDto.from(tasks.save(task));
    }

    public TaskDto update(Long taskId, Long userId, UpdateTaskRequest request) {
        Task task = loadTask(taskId, userId);
        if (request.version() != null && request.version() != task.getVersion()) {
            throw new ConflictException("This task was changed by someone else. Reload and try again.");
        }
        task.setTitle(request.title().trim());
        task.setDescription(request.description());
        task.setStatus(request.status());
        task.setPriority(request.priority());
        task.setDueDate(request.dueDate());
        if (request.assigneeId() == null) {
            task.setAssignee(null);
        } else {
            assign(task, request.assigneeId(), userId);
        }
        // Flush now so the response carries the new version number.
        return TaskDto.from(tasks.saveAndFlush(task));
    }

    /** Used by the board when a card moves between columns. */
    public TaskDto changeStatus(Long taskId, Long userId, TaskStatus status) {
        Task task = loadTask(taskId, userId);
        task.setStatus(status);
        return TaskDto.from(tasks.saveAndFlush(task));
    }

    public void delete(Long taskId, Long userId) {
        Task task = tasks.findById(taskId).orElseThrow(() -> new NotFoundException("Task not found"));
        TeamMember member = access.requireMember(task.getProject().getTeam().getId(), userId);
        boolean isCreator = task.getCreatedBy().getId().equals(userId);
        if (member.getRole() != TeamRole.OWNER && !isCreator) {
            throw new ForbiddenException("Only the task's creator or a team owner can delete it");
        }
        tasks.delete(task);
    }

    private void assign(Task task, Long assigneeId, Long actorId) {
        Long teamId = task.getProject().getTeam().getId();
        User assignee = members.findByTeamIdAndUserId(teamId, assigneeId)
                .map(TeamMember::getUser)
                .orElseThrow(() -> new BadRequestException("The assignee must be a member of the team"));
        boolean changed = task.getAssignee() == null || !task.getAssignee().getId().equals(assigneeId);
        task.setAssignee(assignee);
        // Tell people about new assignments, unless they assigned the task to themselves.
        if (changed && !assigneeId.equals(actorId)) {
            notifications.enqueue(
                    assignee.getEmail(),
                    "New task: " + task.getTitle(),
                    "Hi " + assignee.getDisplayName() + ", you were assigned the task \"" + task.getTitle()
                            + "\" in project \"" + task.getProject().getName() + "\".");
        }
    }

    private Project loadProject(Long projectId, Long userId) {
        Project project = projects.findById(projectId).orElseThrow(() -> new NotFoundException("Project not found"));
        access.requireMember(project.getTeam().getId(), userId);
        return project;
    }

    private Task loadTask(Long taskId, Long userId) {
        Task task = tasks.findById(taskId).orElseThrow(() -> new NotFoundException("Task not found"));
        access.requireMember(task.getProject().getTeam().getId(), userId);
        return task;
    }
}
