package org.example.greenybackend.modules.notification;

import java.util.List;
import org.example.greenybackend.common.response.MessageResponse;
import org.example.greenybackend.domain.entity.UserEntity;
import org.example.greenybackend.modules.notification.dto.UnreadCountResponse;
import org.example.greenybackend.modules.notification.dto.UserNotificationResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user/notifications")
public class UserNotificationController {

    private final UserNotificationService notificationService;

    public UserNotificationController(UserNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<UserNotificationResponse> notifications(
            @AuthenticationPrincipal(expression = "user") UserEntity currentUser
    ) {
        return notificationService.getNotifications(currentUser);
    }

    @GetMapping("/unread-count")
    public UnreadCountResponse unreadCount(@AuthenticationPrincipal(expression = "user") UserEntity currentUser) {
        return new UnreadCountResponse(notificationService.getUnreadCount(currentUser));
    }

    @PatchMapping("/{notificationUserId}/read")
    public UserNotificationResponse markRead(
            @AuthenticationPrincipal(expression = "user") UserEntity currentUser,
            @PathVariable String notificationUserId
    ) {
        return notificationService.markRead(currentUser, notificationUserId);
    }

    @PatchMapping("/read-all")
    public MessageResponse markAllRead(@AuthenticationPrincipal(expression = "user") UserEntity currentUser) {
        notificationService.markAllRead(currentUser);
        return new MessageResponse("Da danh dau tat ca thong bao la da doc");
    }
}
