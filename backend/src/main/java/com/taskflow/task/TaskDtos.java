package com.taskflow.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;

public final class TaskDtos {

    private TaskDtos() {}

    public record TaskDto(
            Long id,
            Long projectId,
            String title,
            String description,
            TaskStatus status,
            TaskPriority priority,
            Long assigneeId,
            String assigneeName,
            Long createdById,
            LocalDate dueDate,
            Instant createdAt,
            Instant updatedAt,
            int version) {

        public static TaskDto from(Task task) {
            return new TaskDto(
                    task.getId(),
                    task.getProject().getId(),
                    task.getTitle(),
                    task.getDescription(),
                    task.getStatus(),
                    task.getPriority(),
                    task.getAssignee() == null ? null : task.getAssignee().getId(),
                    task.getAssignee() == null ? null : task.getAssignee().getDisplayName(),
                    task.getCreatedBy().getId(),
                    task.getDueDate(),
                    task.getCreatedAt(),
                    task.getUpdatedAt(),
                    task.getVersion());
        }
    }

    public record CreateTaskRequest(
            @NotBlank @Size(max = 200) String title,
            @Size(max = 2000) String description,
            TaskPriority priority,
            Long assigneeId,
            LocalDate dueDate) {}

    /**
     * Full replacement of the editable fields (PUT semantics).
     * "version" is the value the client last read. If it no longer matches, someone else has
     * edited the task in the meantime and the update is refused with 409.
     */
    public record UpdateTaskRequest(
            @NotBlank @Size(max = 200) String title,
            @Size(max = 2000) String description,
            @NotNull TaskStatus status,
            @NotNull TaskPriority priority,
            Long assigneeId,
            LocalDate dueDate,
            Integer version) {}

    public record ChangeStatusRequest(@NotNull TaskStatus status) {}
}
