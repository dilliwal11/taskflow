package com.taskflow.team;

import com.taskflow.common.CurrentUser;
import com.taskflow.team.TeamDtos.AddMemberRequest;
import com.taskflow.team.TeamDtos.CreateTeamRequest;
import com.taskflow.team.TeamDtos.MemberDto;
import com.taskflow.team.TeamDtos.TeamDto;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/teams")
public class TeamController {

    private final TeamService teams;

    public TeamController(TeamService teams) {
        this.teams = teams;
    }

    @GetMapping
    public List<TeamDto> list(@AuthenticationPrincipal Jwt jwt) {
        return teams.listForUser(CurrentUser.id(jwt));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TeamDto create(@Valid @RequestBody CreateTeamRequest request, @AuthenticationPrincipal Jwt jwt) {
        return teams.create(CurrentUser.id(jwt), request);
    }

    @GetMapping("/{teamId}")
    public TeamDto get(@PathVariable Long teamId, @AuthenticationPrincipal Jwt jwt) {
        return teams.get(teamId, CurrentUser.id(jwt));
    }

    @GetMapping("/{teamId}/members")
    public List<MemberDto> members(@PathVariable Long teamId, @AuthenticationPrincipal Jwt jwt) {
        return teams.members(teamId, CurrentUser.id(jwt));
    }

    @PostMapping("/{teamId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public MemberDto addMember(
            @PathVariable Long teamId,
            @Valid @RequestBody AddMemberRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return teams.addMember(teamId, CurrentUser.id(jwt), request);
    }

    @DeleteMapping("/{teamId}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(
            @PathVariable Long teamId, @PathVariable Long userId, @AuthenticationPrincipal Jwt jwt) {
        teams.removeMember(teamId, CurrentUser.id(jwt), userId);
    }
}
