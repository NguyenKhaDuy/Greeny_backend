package org.example.greenybackend.modules.notification;

import java.util.List;
import java.util.Optional;
import org.example.greenybackend.domain.entity.Notifications;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationsRepository extends JpaRepository<Notifications, String> {

    @Override
    @EntityGraph(attributePaths = {"notificationUsers", "notificationUsers.userEntity"})
    List<Notifications> findAll();

    @Override
    @EntityGraph(attributePaths = {"notificationUsers", "notificationUsers.userEntity"})
    Optional<Notifications> findById(String notificationId);
}
