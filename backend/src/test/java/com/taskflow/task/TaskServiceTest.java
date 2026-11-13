package com.taskflow.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.taskflow.common.Exceptions.BadRequestException;
import com.taskflow.common.Exceptions.ConflictException;
import com.taskflow.notification.NotificationService;
import com.taskflow.project.Project;
import com.taskflow.project.ProjectRepository;
import com.taskflow.task.TaskDtos.CreateTaskRequest;
import com.taskflow.task.TaskDtos.TaskDto;
import com.taskflow.task.TaskDtos.UpdateTaskRequest;
import com.taskflow.team.Team;
import com.taskflow.team.TeamAccess;
import com.taskflow.team.TeamMember;
import com.taskflow.team.TeamMemberRepository;
import com.taskflow.team.TeamRole;
import com.taskflow.user.User;
import com.taskflow.user.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests: TaskService alone, with every collaborator replaced by a Mockito mock.
 * No Spring context and no database, so these run in milliseconds.
 */
@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    private static final Long TEAM_ID = 10L;
    private static final Long PROJECT_ID = 20L;
    private static final Long ALICE_ID = 1L;
    private static final Long BOB_ID = 2L;

    @Mock
    TaskRepository tasks;

    @Mock
    ProjectRepository projects;

    @Mock
    UserRepository users;

    @Mock
    TeamMemberRepository members;

    @Mock
    TeamAccess access;

    @Mock
    NotificationService notifications;

    @InjectMocks
    TaskService service;

    Team team;
    Project project;
    User alice;
    User bob;

    @BeforeEach
    void setUp() {
        team = withId(new Team("Platform"), TEAM_ID);
        project = withId(new Project(team, "Launch", null), PROJECT_ID);
        alice = withId(new User("alice@example.com", "hash", "Alice"), ALICE_ID);
        bob = withId(new User("bob@example.com", "hash", "Bob"), BOB_ID);
    }

    @Test
    void creatingATaskForAnotherMemberQueuesANotification() {
        when(projects.findById(PROJECT_ID)).thenReturn(Optional.of(project));
        when(users.getReferenceById(ALICE_ID)).thenReturn(alice);
        when(members.findByTeamIdAndUserId(TEAM_ID, BOB_ID))
                .thenReturn(Optional.of(new TeamMember(team, bob, TeamRole.MEMBER)));
        when(tasks.save(any(Task.class))).thenAnswer(invocation -> withId(invocation.getArgument(0), 99L));

        TaskDto created = service.create(
                PROJECT_ID, ALICE_ID, new CreateTaskRequest("Write docs", null, null, BOB_ID, null));

        assertThat(created.id()).isEqualTo(99L);
        assertThat(created.priority()).isEqualTo(TaskPriority.MEDIUM);
        assertThat(created.assigneeName()).isEqualTo("Bob");
        verify(access).requireMember(TEAM_ID, ALICE_ID);
        verify(notifications).enqueue(anyString(), anyString(), anyString());
    }

    @Test
    void assigningSomeoneOutsideTheTeamIsRejected() {
        when(projects.findById(PROJECT_ID)).thenReturn(Optional.of(project));
        when(members.findByTeamIdAndUserId(TEAM_ID, BOB_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(
                        PROJECT_ID, ALICE_ID, new CreateTaskRequest("Write docs", null, null, BOB_ID, null)))
                .isInstanceOf(BadRequestException.class);

        verify(tasks, never()).save(any(Task.class));
    }

    @Test
    void updatingWithAStaleVersionIsAConflict() {
        Task task = withId(new Task(project, "Write docs", null, TaskPriority.MEDIUM, alice), 99L);
        when(tasks.findById(99L)).thenReturn(Optional.of(task));

        UpdateTaskRequest stale =
                new UpdateTaskRequest("Write docs v2", null, TaskStatus.DONE, TaskPriority.LOW, null, null, 7);

        assertThatThrownBy(() -> service.update(99L, ALICE_ID, stale)).isInstanceOf(ConflictException.class);

        assertThat(task.getTitle()).isEqualTo("Write docs");
        verify(tasks, never()).saveAndFlush(any(Task.class));
    }

    /** Entities get their id from the database, so tests set it through reflection. */
    private static <T> T withId(T entity, Long id) {
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }
}
