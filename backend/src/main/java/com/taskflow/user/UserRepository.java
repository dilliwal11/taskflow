package com.taskflow.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data writes the SQL for these methods from their names. */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
