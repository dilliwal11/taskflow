package com.taskflow.notification;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The background half of the outbox pattern: polls for PENDING rows and sends them.
 *
 * Delivery is "at least once". If the app dies after the email goes out but before the
 * transaction commits, the row is still PENDING and is sent again on the next run.
 */
@Component
public class NotificationDispatcher {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatcher.class);
    private static final int BATCH_SIZE = 20;

    private final NotificationRepository notifications;
    private final EmailSender emailSender;

    public NotificationDispatcher(NotificationRepository notifications, EmailSender emailSender) {
        this.notifications = notifications;
        this.emailSender = emailSender;
    }

    /** fixedDelay: wait this long after one run finishes before starting the next. */
    @Scheduled(fixedDelayString = "${app.notifications.poll-interval-ms:5000}")
    @Transactional
    public void dispatchPending() {
        List<Notification> batch = notifications.lockNextBatch(BATCH_SIZE);
        for (Notification notification : batch) {
            try {
                emailSender.send(notification.getRecipient(), notification.getSubject(), notification.getBody());
                notification.markSent();
            } catch (RuntimeException ex) {
                log.warn("Sending notification {} failed: {}", notification.getId(), ex.getMessage());
                notification.markFailed(ex.getMessage());
            }
        }
        // No save() call needed: these entities are managed, so Hibernate writes the
        // changed fields when the transaction commits ("dirty checking").
    }
}
