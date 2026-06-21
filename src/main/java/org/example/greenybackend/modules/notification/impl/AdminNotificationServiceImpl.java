package org.example.greenybackend.modules.notification.impl;

import static org.example.greenybackend.common.util.AdminFilters.contains;
import static org.example.greenybackend.common.util.AdminFilters.dateEquals;
import static org.example.greenybackend.common.util.AdminFilters.isBlankOrAll;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.example.greenybackend.domain.entity.Notifications;
import org.example.greenybackend.domain.entity.NotificationUser;
import org.example.greenybackend.domain.entity.UserEntity;
import org.example.greenybackend.modules.notification.AdminNotificationService;
import org.example.greenybackend.modules.notification.NotificationUserRepository;
import org.example.greenybackend.modules.notification.NotificationsRepository;
import org.example.greenybackend.modules.notification.dto.AdminNotificationRecipientResponse;
import org.example.greenybackend.modules.notification.dto.AdminNotificationRequest;
import org.example.greenybackend.modules.notification.dto.AdminNotificationResponse;
import org.example.greenybackend.modules.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminNotificationServiceImpl implements AdminNotificationService {

    private static final int STATUS_ACTIVE = 1;
    private static final String TARGET_ALL = "ALL";
    private static final String TARGET_ROLE = "ROLE";
    private static final String TARGET_USER = "USER";

    private final NotificationsRepository notificationsRepository;
    private final NotificationUserRepository notificationUserRepository;
    private final UserRepository userRepository;

    public AdminNotificationServiceImpl(
            NotificationsRepository notificationsRepository,
            NotificationUserRepository notificationUserRepository,
            UserRepository userRepository
    ) {
        this.notificationsRepository = notificationsRepository;
        this.notificationUserRepository = notificationUserRepository;
        this.userRepository = userRepository;
    }

    @Override
    public List<UserEntity> getActiveUsers() {
        return userRepository.findByStatus(STATUS_ACTIVE).stream()
                .sorted(Comparator.comparing(UserEntity::getEmail, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
    }

    @Override
    public List<AdminNotificationResponse> getAllNotifications() {
        return getAllNotifications(null, null, null, null, null, null, null);
    }

    @Override
    public List<AdminNotificationResponse> getAllNotifications(
            String title,
            Integer type,
            String recipient,
            String readStatus,
            Integer minRecipients,
            Integer maxRecipients,
            LocalDate created
    ) {
        return notificationsRepository.findAll().stream()
                .sorted(Comparator.comparing(Notifications::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .filter(notification -> contains(notification.getTitle(), title)
                        || contains(notification.getMessageText(), title))
                .filter(notification -> type == null || type.equals(notification.getType()))
                .filter(notification -> matchesRecipient(notification, recipient))
                .filter(notification -> matchesReadStatus(notification, readStatus))
                .filter(notification -> matchesRecipientCount(notification, minRecipients, maxRecipients))
                .filter(notification -> dateEquals(notification.getCreatedAt(), created))
                .map(this::toResponse)
                .toList();
    }

    @Override
    public AdminNotificationResponse getNotification(String notificationId) {
        return toResponse(findNotification(notificationId));
    }

    @Transactional
    @Override
    public AdminNotificationResponse sendNotification(AdminNotificationRequest request) {
        validateRequest(request);
        List<UserEntity> recipients = resolveRecipients(request);
        if (recipients.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy người nhận thông báo");
        }

        LocalDateTime now = LocalDateTime.now();
        Notifications notification = new Notifications();
        notification.setNotificationsId(UUID.randomUUID().toString());
        notification.setType(request.type() == null ? 0 : request.type());
        notification.setTitle(trimToNull(request.title()));
        notification.setMessageText(trimToNull(request.messageText()));
        notification.setData(trimToNull(request.data()));
        notification.setCreatedAt(now);
        notificationsRepository.save(notification);

        String sendingTime = now.toString();
        List<NotificationUser> notificationUsers = recipients.stream()
                .map(user -> {
                    NotificationUser notificationUser = new NotificationUser();
                    notificationUser.setIdNotiUser(UUID.randomUUID().toString());
                    notificationUser.setIsRead(false);
                    notificationUser.setSendingTime(sendingTime);
                    notificationUser.setUserEntity(user);
                    notificationUser.setNotifications(notification);
                    return notificationUser;
                })
                .toList();
        notificationUserRepository.saveAll(notificationUsers);
        notification.getNotificationUsers().addAll(notificationUsers);
        return toResponse(notification);
    }

    @Transactional
    @Override
    public void deleteNotification(String notificationId) {
        Notifications notification = findNotification(notificationId);
        notificationUserRepository.deleteAll(notification.getNotificationUsers());
        notificationsRepository.delete(notification);
    }

    private boolean matchesRecipient(Notifications notification, String recipient) {
        if (recipient == null || recipient.isBlank()) {
            return true;
        }
        return notification.getNotificationUsers().stream()
                .anyMatch(notificationUser -> {
                    UserEntity user = notificationUser.getUserEntity();
                    if (user == null) {
                        return false;
                    }
                    String recipientText = (user.getUserId() == null ? "" : user.getUserId())
                            + " "
                            + (user.getDisplayName() == null ? "" : user.getDisplayName())
                            + " "
                            + (user.getEmail() == null ? "" : user.getEmail());
                    return contains(recipientText, recipient);
                });
    }

    private boolean matchesReadStatus(Notifications notification, String readStatus) {
        if (isBlankOrAll(readStatus)) {
            return true;
        }
        String normalizedStatus = readStatus.trim().toLowerCase();
        List<NotificationUser> recipients = notification.getNotificationUsers();
        return switch (normalizedStatus) {
            case "read" -> !recipients.isEmpty()
                    && recipients.stream().allMatch(recipient -> Boolean.TRUE.equals(recipient.getIsRead()));
            case "unread" -> recipients.stream().anyMatch(recipient -> !Boolean.TRUE.equals(recipient.getIsRead()));
            default -> true;
        };
    }

    private boolean matchesRecipientCount(Notifications notification, Integer minRecipients, Integer maxRecipients) {
        int recipientCount = notification.getNotificationUsers().size();
        return (minRecipients == null || recipientCount >= minRecipients)
                && (maxRecipients == null || recipientCount <= maxRecipients);
    }

    private List<UserEntity> resolveRecipients(AdminNotificationRequest request) {
        String targetType = normalizeTarget(request.targetType());
        if (TARGET_ALL.equals(targetType)) {
            return userRepository.findByStatus(STATUS_ACTIVE);
        }
        if (TARGET_ROLE.equals(targetType)) {
            if (request.role() == null) {
                throw new IllegalArgumentException("Cần chọn nhóm người nhận");
            }
            return userRepository.findByRoleAndStatus(request.role(), STATUS_ACTIVE);
        }
        if (TARGET_USER.equals(targetType)) {
            if (request.userId() != null && !request.userId().isBlank()) {
                return List.of(userRepository.findById(request.userId())
                        .filter(user -> user.getStatus() != null && user.getStatus() == STATUS_ACTIVE)
                        .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người nhận khả dụng")));
            }
            if (request.userEmail() == null || request.userEmail().isBlank()) {
                throw new IllegalArgumentException("Cần nhập email người nhận");
            }
            return List.of(userRepository.findByEmail(request.userEmail().trim())
                    .filter(user -> user.getStatus() != null && user.getStatus() == STATUS_ACTIVE)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người nhận đang hoạt động với email đã nhập")));
        }
        throw new IllegalArgumentException("Kiểu người nhận không hợp lệ");
    }

    private Notifications findNotification(String notificationId) {
        return notificationsRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông báo"));
    }

    private AdminNotificationResponse toResponse(Notifications notification) {
        List<AdminNotificationRecipientResponse> recipients = notification.getNotificationUsers().stream()
                .map(this::toRecipientResponse)
                .toList();
        return new AdminNotificationResponse(
                notification.getNotificationsId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessageText(),
                notification.getData(),
                recipients.size(),
                notification.getCreatedAt(),
                recipients
        );
    }

    private AdminNotificationRecipientResponse toRecipientResponse(NotificationUser notificationUser) {
        UserEntity user = notificationUser.getUserEntity();
        return new AdminNotificationRecipientResponse(
                notificationUser.getIdNotiUser(),
                user == null ? null : user.getUserId(),
                user == null ? null : user.getDisplayName(),
                user == null ? null : user.getEmail(),
                notificationUser.getIsRead(),
                notificationUser.getSendingTime()
        );
    }

    private void validateRequest(AdminNotificationRequest request) {
        if (request.title() == null || request.title().isBlank()) {
            throw new IllegalArgumentException("Tiêu đề thông báo không được để trống");
        }
        if (request.messageText() == null || request.messageText().isBlank()) {
            throw new IllegalArgumentException("Nội dung thông báo không được để trống");
        }
    }

    private String normalizeTarget(String targetType) {
        if (targetType == null || targetType.isBlank()) {
            return TARGET_ALL;
        }
        return targetType.trim().toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
