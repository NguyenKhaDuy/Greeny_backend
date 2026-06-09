package org.example.greenybackend.modules.notification;

import java.time.LocalDate;
import java.util.List;
import org.example.greenybackend.domain.entity.UserEntity;
import org.example.greenybackend.modules.notification.dto.AdminNotificationRequest;
import org.example.greenybackend.modules.notification.dto.AdminNotificationResponse;

public interface AdminNotificationService {

    List<UserEntity> getActiveUsers();

    List<AdminNotificationResponse> getAllNotifications();

    List<AdminNotificationResponse> getAllNotifications(
            String title,
            Integer type,
            String recipient,
            String readStatus,
            Integer minRecipients,
            Integer maxRecipients,
            LocalDate created
    );

    AdminNotificationResponse getNotification(String notificationId);

    AdminNotificationResponse sendNotification(AdminNotificationRequest request);

    void deleteNotification(String notificationId);

}
