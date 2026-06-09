package org.example.greenybackend.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "NOTIFICATION_USER")
public class NotificationUser {

    @Id
    @EqualsAndHashCode.Include
    @Column(name = "ID_NOTI_USER", nullable = false)
    private String idNotiUser;

    @Column(name = "IS_READ")
    private Boolean isRead;

    @Column(name = "SENDING_TIME")
    private String sendingTime;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", nullable = false)
    private UserEntity userEntity;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "NOTIFICATIONS_ID", nullable = false)
    private Notifications notifications;

}
