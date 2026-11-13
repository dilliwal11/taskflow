package com.taskflow.project;

import com.taskflow.common.Exceptions.NotFoundException;
import com.taskflow.project.ProjectDtos.CreateProjectRequest;
import com.taskflow.project.ProjectDtos.ProjectDto;
import com.taskflow.team.TeamAccess;
import com.taskflow.team.TeamMember;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProjectService {

    private final ProjectRepository projects;
    private final TeamAccess access;

    public ProjectService(ProjectRepository projects, TeamAccess access) {
        this.projects = projects;
        this.access = access;
    }

    @Transactional(readOnly = true)
    public List<ProjectDto> list(Long teamId, Long userId) {
        access.requireMember(teamId, userId);
        return projects.findByTeamIdOrderByCreatedAtDesc(teamId).stream().map(ProjectDto::from).toList();
    }

    public ProjectDto create(Long teamId, Long userId, CreateProjectRequest request) {
        TeamMember member = access.requireMember(teamId, userId);
        Project project = projects.save(new Project(member.getTeam(), request.name().trim(), request.description()));
        return ProjectDto.from(project);
    }

    @Transactional(readOnly = true)
    public ProjectDto get(Long projectId, Long userId) {
        Project project = find(projectId);
        access.requireMember(project.getTeam().getId(), userId);
        return ProjectDto.from(project);
    }

    public void delete(Long projectId, Long userId) {
        Project project = find(projectId);
        access.requireOwner(project.getTeam().getId(), userId);
        // The tasks go too: the foreign key is declared "on delete cascade" in V1.
        projects.delete(project);
    }

    private Project find(Long projectId) {
        return projects.findById(projectId).orElseThrow(() -> new NotFoundException("Project not found"));
    }
}
