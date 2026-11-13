package com.taskflow.team;

import com.taskflow.common.Exceptions.ForbiddenException;
import org.springframework.stereotype.Component;

/**
 * Authorization checks, used by every service before it touches team data.
 *
 * The JWT only proves who the caller is. Whether they may see team 42 depends on a database
 * row, so it is checked here on each request rather than baked into the token.
 */
@Component
public class TeamAccess {

    private final TeamMemberRepository members;

    public TeamAccess(TeamMemberRepository members) {
        this.members = members;
    }

    public TeamMember requireMember(Long teamId, Long userId) {
        return members.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new ForbiddenException("You are not a member of this team"));
    }

    public TeamMember requireOwner(Long teamId, Long userId) {
        TeamMember member = requireMember(teamId, userId);
        if (member.getRole() != TeamRole.OWNER) {
            throw new ForbiddenException("Only a team owner can do this");
        }
        return member;
    }
}
