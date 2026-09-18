package com.smartcampus.service;

import com.smartcampus.model.Notification;
import com.smartcampus.model.enums.NotificationType;
import com.smartcampus.repository.NotificationRepository;
import com.smartcampus.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public Notification notify(String recipientUserId, NotificationType type, String title, String message) {
        if (recipientUserId == null) return null;
        Notification notification = Notification.builder()
                .recipientUserId(recipientUserId)
                .type(type)
                .title(title)
                .message(message)
                .read(false)
                .createdAt(LocalDateTime.now())
                .build();
        return notificationRepository.save(notification);
    }

    /**
     * Same as notify(), but also emails the user (using their login email) with a longer,
     * more detailed body than the in-app notification message. Used for things like
     * shortlist results where the student needs company/round/login details in their inbox.
     */
    public Notification notifyAndEmail(String recipientUserId, NotificationType type, String title,
                                        String inAppMessage, String emailSubject, String emailBody) {
        return notifyAndEmail(recipientUserId, null, type, title, inAppMessage, emailSubject, emailBody);
    }

    /**
     * Overload for recipients who may not have a login account yet (e.g. a student the T&amp;P
     * office only ever bulk-imported): the email still goes out to fallbackEmail, and the
     * in-app notification is simply skipped.
     */
    public Notification notifyAndEmail(String recipientUserId, String fallbackEmail, NotificationType type,
                                        String title, String inAppMessage, String emailSubject, String emailBody) {
        Notification notification = notify(recipientUserId, type, title, inAppMessage);
        String address = recipientUserId == null ? null : userRepository.findById(recipientUserId)
                .map(user -> user.getEmail()).orElse(null);
        if (address == null || address.isBlank()) {
            address = fallbackEmail;
        }
        emailService.send(address, emailSubject, emailBody);
        return notification;
    }

    public List<Notification> getForUser(String userId) {
        return notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(userId);
    }

    public long unreadCount(String userId) {
        return notificationRepository.countByRecipientUserIdAndReadFalse(userId);
    }

    public Notification markRead(String id) {
        Notification n = notificationRepository.findById(id).orElseThrow();
        n.setRead(true);
        return notificationRepository.save(n);
    }
}
