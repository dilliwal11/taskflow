package com.taskflow.project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class ProjectDtos {

    private ProjectDtos() {}

    public record ProjectDto(Long id, Long teamId, String name, String description, Instant createdAt) {

        public static ProjectDto from(Project project) {
            return new ProjectDto(
                    project.getId(),
                    project.getTeam().getId(),
                    project.getName(),
                    project.getDescription(),
                    project.getCreatedAt());
        }
    }

    public record CreateProjectRequest(@NotBlank @Size(max = 120) String name, @Size(max = 2000) String description) {}
}
