package org.example.greenybackend.modules.notification.impl;

import org.example.greenybackend.modules.notification.UserNotificationService;
import org.example.greenybackend.modules.notification.NotificationUserRepository;
import org.example.greenybackend.modules.notification.NotificationsRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.example.greenybackend.domain.entity.Notifications;
import org.example.greenybackend.domain.entity.NotificationUser;
import org.example.greenybackend.domain.entity.UserEntity;
import org.example.greenybackend.modules.notification.dto.UserNotificationResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserNotificationServiceImpl implements UserNotificationService {

    private final NotificationsRepository notificationsRepository;
    private final NotificationUserRepository notificationUserRepository;

    public UserNotificationServiceImpl(
            NotificationsRepository notificationsRepository,
            NotificationUserRepository notificationUserRepository
    ) {
        this.notificationsRepository = notificationsRepository;
        this.notificationUserRepository = notificationUserRepository;
    }

    @Transactional(readOnly = true)
    @Override
    public List<UserNotificationResponse> getNotifications(UserEntity user) {
        return notificationUserRepository.findByUserEntityUserIdOrderBySendingTimeDesc(user.getUserId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public long getUnreadCount(UserEntity user) {
        return notificationUserRepository.countByUserEntityUserIdAndIsReadFalse(user.getUserId());
    }

    @Transactional
    @Override
    public UserNotificationResponse markRead(UserEntity user, String notificationUserId) {
        NotificationUser notificationUser = notificationUserRepository
                .findByIdNotiUserAndUserEntityUserId(notificationUserId, user.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay thong bao"));
        notificationUser.setIsRead(true);
        return toResponse(notificationUser);
    }

    @Transactional
    @Override
    public void markAllRead(UserEntity user) {
        notificationUserRepository.findByUserEntityUserIdOrderBySendingTimeDesc(user.getUserId())
                .forEach(notificationUser -> notificationUser.setIsRead(true));
    }

    @Transactional
    @Override
    public UserNotificationResponse sendToUser(UserEntity user, Integer type, String title, String message, String data) {
        if (user == null || user.getUserId() == null) {
            throw new IllegalArgumentException("Nguoi nhan thong bao khong hop le");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Tieu de thong bao khong duoc de trong");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Noi dung thong bao khong duoc de trong");
        }

        LocalDateTime now = LocalDateTime.now();
        Notifications notification = new Notifications();
        notification.setNotificationsId(UUID.randomUUID().toString());
        notification.setType(type == null ? 0 : type);
        notification.setTitle(trimToLength(title, 50));
        notification.setMessageText(message.trim());
        notification.setData(trimToNull(data));
        notification.setCreatedAt(now);
        notificationsRepository.save(notification);

        NotificationUser notificationUser = new NotificationUser();
        notificationUser.setIdNotiUser(UUID.randomUUID().toString());
        notificationUser.setNotifications(notification);
        notificationUser.setUserEntity(user);
        notificationUser.setIsRead(false);
        notificationUser.setSendingTime(now.toString());
        notificationUserRepository.save(notificationUser);
        notification.getNotificationUsers().add(notificationUser);
        return toResponse(notificationUser);
    }

    private UserNotificationResponse toResponse(NotificationUser notificationUser) {
        Notifications notification = notificationUser.getNotifications();
        return new UserNotificationResponse(
                notificationUser.getIdNotiUser(),
                notification == null ? null : notification.getNotificationsId(),
                notification == null ? null : notification.getType(),
                notification == null ? null : notification.getTitle(),
                notification == null ? null : notification.getMessageText(),
                notification == null ? null : notification.getData(),
                notificationUser.getIsRead(),
                notificationUser.getSendingTime(),
                notification == null ? null : notification.getCreatedAt()
        );
    }

    private String trimToLength(String value, int maxLength) {
        String trimmed = value.trim();
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, maxLength);
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
