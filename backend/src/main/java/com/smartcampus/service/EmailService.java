package com.smartcampus.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Thin wrapper around JavaMailSender.
 *
 * app.mail.enabled must be true (and MAIL_USERNAME/MAIL_PASSWORD configured) for real emails
 * to go out. When disabled (the default in dev, since no SMTP creds are configured), every
 * "send" is just logged so the rest of the shortlist/login-provisioning flow keeps working
 * without a mail server. This means the app never breaks or slows down when mail isn't set up.
 */
@Service
@RequiredArgsConstructor
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${app.mail.from}")
    private String fromAddress;

    public void send(String to, String subject, String body) {
        if (to == null || to.isBlank()) {
            log.warn("Skipping email '{}' — recipient has no email address on file.", subject);
            return;
        }

        if (!mailEnabled) {
            log.info("[MAIL DISABLED] Would send to {} | subject: {}\n{}", to, subject, body);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email sent to {} | subject: {}", to, subject);
        } catch (Exception ex) {
            // Never let a mail-server hiccup break shortlisting/login-provisioning.
            log.error("Failed to send email to {} (subject: {}): {}", to, subject, ex.getMessage());
        }
    }
}
