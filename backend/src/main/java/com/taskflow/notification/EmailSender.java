package com.taskflow.notification;

/**
 * The dispatcher depends on this interface, not on SMTP. Spring injects whichever
 * implementation is active, and tests can supply a fake.
 */
public interface EmailSender {

    void send(String to, String subject, String body);
}
