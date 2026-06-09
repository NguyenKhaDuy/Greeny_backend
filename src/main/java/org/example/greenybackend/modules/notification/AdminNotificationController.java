package org.example.greenybackend.modules.notification;

import java.time.LocalDate;
import java.util.List;
import org.example.greenybackend.common.response.MessageResponse;
import org.example.greenybackend.modules.notification.dto.AdminNotificationRequest;
import org.example.greenybackend.modules.notification.dto.AdminNotificationResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/notifications")
public class AdminNotificationController {

    private final AdminNotificationService notificationService;

    public AdminNotificationController(AdminNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<AdminNotificationResponse> getAll(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) String recipient,
            @RequestParam(required = false) String readStatus,
            @RequestParam(required = false) Integer minRecipients,
            @RequestParam(required = false) Integer maxRecipients,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate created
    ) {
        return notificationService.getAllNotifications(title, type, recipient, readStatus, minRecipients, maxRecipients, created);
    }

    @GetMapping("/{notificationId}")
    public AdminNotificationResponse getById(@PathVariable String notificationId) {
        return notificationService.getNotification(notificationId);
    }

    @PostMapping
    public ResponseEntity<AdminNotificationResponse> send(@RequestBody AdminNotificationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(notificationService.sendNotification(request));
    }

    @DeleteMapping("/{notificationId}")
    public MessageResponse delete(@PathVariable String notificationId) {
        notificationService.deleteNotification(notificationId);
        return new MessageResponse("Da xoa thong bao.");
    }
}
