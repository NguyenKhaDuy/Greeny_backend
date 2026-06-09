package org.example.greenybackend.domain.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "NOTIFICATIONS")
public class Notifications {

    @Id
    @EqualsAndHashCode.Include
    @Column(name = "NOTIFICATIONS_ID", nullable = false)
    private String notificationsId;

    @Column(name = "TYPE")
    private Integer type;

    @Column(name = "TITLE", length = 50)
    private String title;

    @Column(name = "MESSAGE_TEXT")
    private String messageText;

    @Column(name = "DATA")
    private String data;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "notifications")
    @Builder.Default
    private List<NotificationUser> notificationUsers = new ArrayList<>();

}
