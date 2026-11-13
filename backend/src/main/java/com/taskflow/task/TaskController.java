package com.taskflow.task;

import com.taskflow.common.CurrentUser;
import com.taskflow.task.TaskDtos.ChangeStatusRequest;
import com.taskflow.task.TaskDtos.CreateTaskRequest;
import com.taskflow.task.TaskDtos.TaskDto;
import com.taskflow.task.TaskDtos.UpdateTaskRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TaskController {

    private final TaskService tasks;

    public TaskController(TaskService tasks) {
        this.tasks = tasks;
    }

    /** Example: GET /api/projects/7/tasks?status=TODO&assigneeId=3 */
    @GetMapping("/api/projects/{projectId}/tasks")
    public List<TaskDto> list(
            @PathVariable Long projectId,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) Long assigneeId,
            @AuthenticationPrincipal Jwt jwt) {
        return tasks.list(projectId, CurrentUser.id(jwt), status, assigneeId);
    }

    @PostMapping("/api/projects/{projectId}/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    public TaskDto create(
            @PathVariable Long projectId,
            @Valid @RequestBody CreateTaskRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return tasks.create(projectId, CurrentUser.id(jwt), request);
    }

    /** PUT replaces all editable fields. */
    @PutMapping("/api/tasks/{taskId}")
    public TaskDto update(
            @PathVariable Long taskId,
            @Valid @RequestBody UpdateTaskRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return tasks.update(taskId, CurrentUser.id(jwt), request);
    }

    /** PATCH changes one thing: the status. */
    @PatchMapping("/api/tasks/{taskId}/status")
    public TaskDto changeStatus(
            @PathVariable Long taskId,
            @Valid @RequestBody ChangeStatusRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return tasks.changeStatus(taskId, CurrentUser.id(jwt), request.status());
    }

    @DeleteMapping("/api/tasks/{taskId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long taskId, @AuthenticationPrincipal Jwt jwt) {
        tasks.delete(taskId, CurrentUser.id(jwt));
    }
}
