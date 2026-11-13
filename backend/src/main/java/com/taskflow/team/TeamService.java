package com.taskflow.team;

import com.taskflow.common.Exceptions.ConflictException;
import com.taskflow.common.Exceptions.NotFoundException;
import com.taskflow.notification.NotificationService;
import com.taskflow.team.TeamDtos.AddMemberRequest;
import com.taskflow.team.TeamDtos.CreateTeamRequest;
import com.taskflow.team.TeamDtos.MemberDto;
import com.taskflow.team.TeamDtos.TeamDto;
import com.taskflow.user.User;
import com.taskflow.user.UserRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @Transactional on the class: every public method runs in one database transaction.
 * If the method throws, everything it wrote is rolled back together.
 */
@Service
@Transactional
public class TeamService {

    private final TeamRepository teams;
    private final TeamMemberRepository members;
    private final UserRepository users;
    private final TeamAccess access;
    private final NotificationService notifications;

    public TeamService(
            TeamRepository teams,
            TeamMemberRepository members,
            UserRepository users,
            TeamAccess access,
            NotificationService notifications) {
        this.teams = teams;
        this.members = members;
        this.users = users;
        this.access = access;
        this.notifications = notifications;
    }

    @Transactional(readOnly = true)
    public List<TeamDto> listForUser(Long userId) {
        return members.findByUserIdWithTeam(userId).stream().map(TeamDto::from).toList();
    }

    /** Creates the team and makes the creator its owner. Both inserts commit or neither does. */
    public TeamDto create(Long userId, CreateTeamRequest request) {
        Team team = teams.save(new Team(request.name().trim()));
        // getReferenceById returns a proxy holding just the id: enough for a foreign key, no SELECT needed.
        User creator = users.getReferenceById(userId);
        TeamMember membership = members.save(new TeamMember(team, creator, TeamRole.OWNER));
        return TeamDto.from(membership);
    }

    @Transactional(readOnly = true)
    public TeamDto get(Long teamId, Long userId) {
        return TeamDto.from(access.requireMember(teamId, userId));
    }

    @Transactional(readOnly = true)
    public List<MemberDto> members(Long teamId, Long userId) {
        access.requireMember(teamId, userId);
        return members.findByTeamIdWithUser(teamId).stream().map(MemberDto::from).toList();
    }

    public MemberDto addMember(Long teamId, Long actorId, AddMemberRequest request) {
        TeamMember actor = access.requireOwner(teamId, actorId);
        User user = users.findByEmail(request.email().trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new NotFoundException("No user with that email. They need to register first."));
        if (members.existsByTeamIdAndUserId(teamId, user.getId())) {
            throw new ConflictException("This user is already in the team");
        }
        TeamMember membership = members.save(new TeamMember(actor.getTeam(), user, request.role()));
        // Saved in this same transaction. The email itself goes out later (see NotificationDispatcher).
        notifications.enqueue(
                user.getEmail(),
                "You were added to " + actor.getTeam().getName(),
                "Hi " + user.getDisplayName() + ", you are now a " + request.role().name().toLowerCase(Locale.ROOT)
                        + " of the team \"" + actor.getTeam().getName() + "\" on Taskflow.");
        return MemberDto.from(membership);
    }

    public void removeMember(Long teamId, Long actorId, Long userId) {
        access.requireOwner(teamId, actorId);
        TeamMember target = members.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new NotFoundException("This user is not in the team"));
        if (target.getRole() == TeamRole.OWNER && members.countByTeamIdAndRole(teamId, TeamRole.OWNER) <= 1) {
            throw new ConflictException("A team must keep at least one owner");
        }
        members.delete(target);
    }
}
