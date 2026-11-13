package com.taskflow.notification;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * Native SQL, because "for update skip locked" is a Postgres feature JPQL cannot express.
     *
     * "for update" locks the selected rows until the transaction ends. "skip locked" makes a
     * second app instance skip rows the first one already holds instead of waiting for them.
     * Together they let several instances share the queue without sending an email twice.
     */
    @Query(
            value = "select * from notifications where status = 'PENDING' "
                    + "order by created_at limit :batchSize for update skip locked",
            nativeQuery = true)
    List<Notification> lockNextBatch(@Param("batchSize") int batchSize);

    long countByRecipient(String recipient);
}
