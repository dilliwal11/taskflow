package com.taskflow.project;

import com.taskflow.common.CurrentUser;
import com.taskflow.project.ProjectDtos.CreateProjectRequest;
import com.taskflow.project.ProjectDtos.ProjectDto;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Projects are nested under a team for listing and creating, and addressed by their own id after that. */
@RestController
public class ProjectController {

    private final ProjectService projects;

    public ProjectController(ProjectService projects) {
        this.projects = projects;
    }

    @GetMapping("/api/teams/{teamId}/projects")
    public List<ProjectDto> list(@PathVariable Long teamId, @AuthenticationPrincipal Jwt jwt) {
        return projects.list(teamId, CurrentUser.id(jwt));
    }

    @PostMapping("/api/teams/{teamId}/projects")
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectDto create(
            @PathVariable Long teamId,
            @Valid @RequestBody CreateProjectRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return projects.create(teamId, CurrentUser.id(jwt), request);
    }

    @GetMapping("/api/projects/{projectId}")
    public ProjectDto get(@PathVariable Long projectId, @AuthenticationPrincipal Jwt jwt) {
        return projects.get(projectId, CurrentUser.id(jwt));
    }

    @DeleteMapping("/api/projects/{projectId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long projectId, @AuthenticationPrincipal Jwt jwt) {
        projects.delete(projectId, CurrentUser.id(jwt));
    }
}
