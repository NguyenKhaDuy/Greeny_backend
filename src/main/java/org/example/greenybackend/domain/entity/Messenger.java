package org.example.greenybackend.domain.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "MESSENGER")
public class Messenger {

    @Id
    @EqualsAndHashCode.Include
    @Column(name = "ID_MESSENGER", nullable = false)
    private String idMessenger;

    @Column(name = "SENDER_TYPE")
    private String senderType;

    @Column(name = "MESSAGE_TEXT", columnDefinition = "TEXT")
    private String messageText;

    @Column(name = "INTENT_DETECTED")
    private String intentDetected;

    @Column(name = "EXTRACTED_DATA")
    private String extractedData;

    @Column(name = "CONFIDENT_SCORE")
    private Integer confidentScore;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", nullable = false)
    private UserEntity userEntity;

}
