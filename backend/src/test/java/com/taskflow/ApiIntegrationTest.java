package com.taskflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.notification.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration tests: the whole application (security filters, controllers, services, JPA,
 * Flyway migrations) running against a real PostgreSQL started in Docker by Testcontainers.
 *
 * Slower than unit tests, but they catch what mocks cannot: wrong SQL, a migration that does
 * not match an entity, a security rule that lets the wrong person in.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ApiIntegrationTest {

    // @ServiceConnection points spring.datasource.* at this container automatically.
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @Autowired
    NotificationRepository notifications;

    @Test
    void requestsWithoutATokenAreRejected() throws Exception {
        mvc.perform(get("/api/teams")).andExpect(status().isUnauthorized());
    }

    @Test
    void registrationValidatesInput() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\",\"password\":\"short\",\"displayName\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void duplicateEmailIsAConflictAndWrongPasswordIsUnauthorized() throws Exception {
        register("dup@example.com", "Dup");

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"dup@example.com\",\"password\":\"password123\",\"displayName\":\"Dup\"}"))
                .andExpect(status().isConflict());

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"dup@example.com\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"dup@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void ownerAndMemberWorkflow() throws Exception {
        JsonNode alice = register("alice@example.com", "Alice");
        JsonNode bob = register("bob@example.com", "Bob");
        String aliceToken = alice.get("token").asText();
        String bobToken = bob.get("token").asText();
        long bobId = bob.get("user").get("id").asLong();

        // Alice creates a team and becomes its owner.
        JsonNode team = body(mvc.perform(authed(post("/api/teams"), aliceToken).content("{\"name\":\"Platform\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("OWNER"))
                .andReturn()
                .getResponse()
                .getContentAsString());
        long teamId = team.get("id").asLong();

        JsonNode project = body(mvc.perform(authed(post("/api/teams/" + teamId + "/projects"), aliceToken)
                        .content("{\"name\":\"Launch\",\"description\":\"Q1 launch\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString());
        long projectId = project.get("id").asLong();

        // Bob is not in the team yet: authenticated, but forbidden.
        mvc.perform(authed(get("/api/teams/" + teamId + "/projects"), bobToken))
                .andExpect(status().isForbidden());
        mvc.perform(authed(get("/api/projects/" + projectId + "/tasks"), bobToken))
                .andExpect(status().isForbidden());

        // Alice adds Bob as a member. That queues an email in the outbox table.
        mvc.perform(authed(post("/api/teams/" + teamId + "/members"), aliceToken)
                        .content("{\"email\":\"bob@example.com\",\"role\":\"MEMBER\"}"))
                .andExpect(status().isCreated());
        assertThat(notifications.countByRecipient("bob@example.com")).isEqualTo(1);

        mvc.perform(authed(get("/api/teams/" + teamId + "/projects"), bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        // Alice creates a task assigned to Bob (a second email for Bob).
        JsonNode task = body(mvc.perform(authed(post("/api/projects/" + projectId + "/tasks"), aliceToken)
                        .content("{\"title\":\"Write docs\",\"priority\":\"HIGH\",\"assigneeId\":" + bobId + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.assigneeName").value("Bob"))
                .andReturn()
                .getResponse()
                .getContentAsString());
        long taskId = task.get("id").asLong();
        int version = task.get("version").asInt();
        assertThat(notifications.countByRecipient("bob@example.com")).isEqualTo(2);

        // Bob moves the task. The version number goes up.
        mvc.perform(authed(patch("/api/tasks/" + taskId + "/status"), bobToken).content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.version").value(version + 1));

        // Alice still holds the old version: her edit is refused instead of overwriting Bob's.
        String staleUpdate = "{\"title\":\"Write docs v2\",\"status\":\"TODO\",\"priority\":\"LOW\",\"version\":" + version + "}";
        mvc.perform(authed(put("/api/tasks/" + taskId), aliceToken).content(staleUpdate))
                .andExpect(status().isConflict());

        // Filtering by status.
        mvc.perform(authed(get("/api/projects/" + projectId + "/tasks?status=IN_PROGRESS"), bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        mvc.perform(authed(get("/api/projects/" + projectId + "/tasks?status=DONE"), bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // Members cannot delete projects or manage members. Owners can.
        mvc.perform(authed(delete("/api/projects/" + projectId), bobToken)).andExpect(status().isForbidden());
        mvc.perform(authed(post("/api/teams/" + teamId + "/members"), bobToken)
                        .content("{\"email\":\"alice@example.com\",\"role\":\"MEMBER\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(authed(delete("/api/projects/" + projectId), aliceToken)).andExpect(status().isNoContent());

        // The project's tasks were removed with it (on delete cascade).
        mvc.perform(authed(get("/api/projects/" + projectId + "/tasks"), aliceToken))
                .andExpect(status().isNotFound());
    }

    private JsonNode register(String email, String name) throws Exception {
        String request = "{\"email\":\"" + email + "\",\"password\":\"password123\",\"displayName\":\"" + name + "\"}";
        String response = mvc.perform(
                        post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return body(response);
    }

    private JsonNode body(String response) throws Exception {
        return json.readTree(response);
    }

    private static MockHttpServletRequestBuilder authed(MockHttpServletRequestBuilder request, String token) {
        return request.header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON);
    }
}
