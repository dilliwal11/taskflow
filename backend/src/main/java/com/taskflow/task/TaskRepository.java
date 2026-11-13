package com.taskflow.task;

import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

    // @EntityGraph tells Hibernate to fetch each task's assignee in the same query.
    // The board shows the assignee's name on every card, so this avoids an N+1.
    @Override
    @EntityGraph(attributePaths = "assignee")
    List<Task> findAll(Specification<Task> spec, Sort sort);
}
