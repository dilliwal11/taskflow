package com.taskflow.team;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class TeamDtos {

    private TeamDtos() {}

    /** A team as seen by one user, including that user's role in it. */
    public record TeamDto(Long id, String name, TeamRole role) {

        public static TeamDto from(TeamMember membership) {
            return new TeamDto(membership.getTeam().getId(), membership.getTeam().getName(), membership.getRole());
        }
    }

    public record MemberDto(Long userId, String email, String displayName, TeamRole role) {

        public static MemberDto from(TeamMember membership) {
            return new MemberDto(
                    membership.getUser().getId(),
                    membership.getUser().getEmail(),
                    membership.getUser().getDisplayName(),
                    membership.getRole());
        }
    }

    public record CreateTeamRequest(@NotBlank @Size(max = 100) String name) {}

    public record AddMemberRequest(@NotBlank @Email String email, @NotNull TeamRole role) {}
}
