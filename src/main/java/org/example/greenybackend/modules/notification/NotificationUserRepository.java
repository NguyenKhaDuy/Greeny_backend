package org.example.greenybackend.modules.notification;

import java.util.List;
import java.util.Optional;
import org.example.greenybackend.domain.entity.NotificationUser;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationUserRepository extends JpaRepository<NotificationUser, String> {

    @EntityGraph(attributePaths = {"notifications"})
    List<NotificationUser> findByUserEntityUserIdOrderBySendingTimeDesc(String userId);

    @EntityGraph(attributePaths = {"notifications"})
    Optional<NotificationUser> findByIdNotiUserAndUserEntityUserId(String notificationUserId, String userId);

    long countByUserEntityUserIdAndIsReadFalse(String userId);
}
