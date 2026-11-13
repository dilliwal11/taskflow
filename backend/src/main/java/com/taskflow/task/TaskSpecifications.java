package com.taskflow.task;

import org.springframework.data.jpa.domain.Specification;

/**
 * Small reusable WHERE-clause pieces. TaskService combines them with .and() depending on which
 * filters the caller sent, which is cleaner than one query method per filter combination.
 */
public final class TaskSpecifications {

    private TaskSpecifications() {}

    public static Specification<Task> inProject(Long projectId) {
        return (root, query, cb) -> cb.equal(root.get("project").get("id"), projectId);
    }

    public static Specification<Task> hasStatus(TaskStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Task> assignedTo(Long userId) {
        return (root, query, cb) -> cb.equal(root.get("assignee").get("id"), userId);
    }
}
