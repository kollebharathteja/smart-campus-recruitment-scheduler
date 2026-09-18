package com.smartcampus.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Sends email via Brevo's HTTPS REST API instead of raw SMTP.
 * Render's free tier blocks outbound SMTP (ports 587/465/25), so JavaMailSender never
 * worked in production even with correct Gmail credentials. Brevo's API runs over plain
 * HTTPS (port 443), which Render allows, so this is the fix — not a workaround.
 *
 * app.mail.enabled + app.mail.brevo-api-key must both be set for real emails to go out.
 * Otherwise every "send" is just logged, so nothing breaks when unconfigured.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final URI BREVO_ENDPOINT = URI.create("https://api.brevo.com/v3/smtp/email");

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${app.mail.brevo-api-key:}")
    private String brevoApiKey;

    @Value("${app.mail.from-email}")
    private String fromEmail;

    @Value("${app.mail.from-name:Smart Campus Recruitment}")
    private String fromName;

    /** A blank or leftover placeholder value counts as "not configured". */
    private boolean hasUsableKey() {
        return brevoApiKey != null && !brevoApiKey.isBlank() && brevoApiKey.startsWith("xkeysib-");
    }

    public void send(String to, String subject, String body) {
        if (to == null || to.isBlank()) {
            log.warn("Skipping email '{}' — recipient has no email address on file.", subject);
            return;
        }

        if (!mailEnabled || !hasUsableKey()) {
            log.warn("[MAIL DISABLED] Would send to {} | subject: {} — set BREVO_API_KEY (an xkeysib-… key) "
                    + "and MAIL_ENABLED=true for real delivery.\n{}", to, subject, body);
            return;
        }

        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            Map<String, String> sender = new LinkedHashMap<>();
            sender.put("name", fromName);
            sender.put("email", fromEmail);
            payload.put("sender", sender);
            payload.put("to", List.of(Map.of("email", to)));
            payload.put("subject", subject);
            payload.put("textContent", body);

            String json = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(BREVO_ENDPOINT)
                    .timeout(Duration.ofSeconds(15))
                    .header("accept", "application/json")
                    .header("content-type", "application/json")
                    .header("api-key", brevoApiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("Email sent to {} | subject: {} | Brevo response: {}", to, subject, response.body());
            } else {
                log.error("Brevo rejected email to {} (subject: {}): HTTP {} — {}",
                        to, subject, response.statusCode(), response.body());
            }
        } catch (Exception ex) {
            log.error("Failed to send email to {} (subject: {}): {}", to, subject, ex.getMessage());
        }
    }
}