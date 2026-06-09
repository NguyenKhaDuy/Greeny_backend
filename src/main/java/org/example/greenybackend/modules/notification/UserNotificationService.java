package org.example.greenybackend.modules.notification;

import java.util.List;
import org.example.greenybackend.domain.entity.UserEntity;
import org.example.greenybackend.modules.notification.dto.UserNotificationResponse;

public interface UserNotificationService {

    List<UserNotificationResponse> getNotifications(UserEntity user);

    long getUnreadCount(UserEntity user);

    UserNotificationResponse markRead(UserEntity user, String notificationUserId);

    void markAllRead(UserEntity user);

    UserNotificationResponse sendToUser(UserEntity user, Integer type, String title, String message, String data);

}
