package com.taskflow.team;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {

    Optional<TeamMember> findByTeamIdAndUserId(Long teamId, Long userId);

    boolean existsByTeamIdAndUserId(Long teamId, Long userId);

    long countByTeamIdAndRole(Long teamId, TeamRole role);

    // "join fetch" loads the team in the same SQL statement. Without it, reading m.getTeam().getName()
    // for N memberships fires N extra queries: the classic N+1 problem.
    @Query("select m from TeamMember m join fetch m.team t where m.user.id = :userId order by t.name")
    List<TeamMember> findByUserIdWithTeam(@Param("userId") Long userId);

    @Query("select m from TeamMember m join fetch m.user where m.team.id = :teamId order by m.joinedAt")
    List<TeamMember> findByTeamIdWithUser(@Param("teamId") Long teamId);
}
