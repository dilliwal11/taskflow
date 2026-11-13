package com.taskflow.notification;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The "outbox" half of the outbox pattern.
 *
 * Calling an SMTP server inside a business transaction is risky: the email may go out and the
 * transaction then roll back, or the mail server may be slow and hold a database connection.
 * Instead we only insert a row. It commits or rolls back together with the business change,
 * and NotificationDispatcher sends it afterwards.
 */
@Service
public class NotificationService {

    private final NotificationRepository notifications;

    public NotificationService(NotificationRepository notifications) {
        this.notifications = notifications;
    }

    /** MANDATORY: refuses to run outside a caller's transaction, since that would defeat the point. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueue(String recipient, String subject, String body) {
        notifications.save(new Notification(recipient, subject, body));
    }
}
