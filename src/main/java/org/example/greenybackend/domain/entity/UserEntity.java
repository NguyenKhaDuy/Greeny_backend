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
@Table(name = "USER")
public class UserEntity {

    @Id
    @EqualsAndHashCode.Include
    @Column(name = "USER_ID", nullable = false)
    private String userId;

    @Column(name = "TITLE", length = 50)
    private String title;

    @Column(name = "EMAIL", length = 100, unique = true)
    private String email;

    @Column(name = "PASS", length = 255)
    private String pass;

    @Column(name = "PHONE", length = 11)
    private String phone;

    @Column(name = "AVATAR", length = 255)
    private String avatar;

    @Column(name = "ROLE")
    private Integer role;

    @Column(name = "STATUS")
    private Integer status;

    @Column(name = "EMAIL_VERAT")
    private LocalDateTime emailVerat;

    @Column(name = "RE_TOKEN", length = 1024)
    private String reToken;

    @Column(name = "LASTLOGIN")
    private LocalDateTime lastlogin;

    @Column(name = "CREATEAT")
    private LocalDateTime createat;

    @Column(name = "UPDATEAT")
    private LocalDateTime updateat;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "userEntity")
    @Builder.Default
    private List<CartItem> cartItems = new ArrayList<>();

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "userEntity")
    @Builder.Default
    private List<Favorite> favorites = new ArrayList<>();

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "userEntity")
    @Builder.Default
    private List<CouponUsages> couponUsagesList = new ArrayList<>();

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "userEntity")
    @Builder.Default
    private List<Address> addressList = new ArrayList<>();

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "userEntity")
    @Builder.Default
    private List<Orders> ordersList = new ArrayList<>();

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "userEntity")
    @Builder.Default
    private List<ReturnRequests> returnRequestsList = new ArrayList<>();

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "userEntity")
    @Builder.Default
    private List<ProductReviews> productReviewsList = new ArrayList<>();

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "userEntity")
    @Builder.Default
    private List<AiUsageLogs> aiUsageLogsList = new ArrayList<>();

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "userEntity")
    @Builder.Default
    private List<NotificationUser> notificationUsers = new ArrayList<>();

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "userEntity")
    @Builder.Default
    private List<PlantCareArticles> plantCareArticlesList = new ArrayList<>();

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "userEntity")
    @Builder.Default
    private List<AiSettings> aiSettingsList = new ArrayList<>();

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "userEntity")
    @Builder.Default
    private List<AiChat> aiChats = new ArrayList<>();

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "userEntity")
    @Builder.Default
    private List<Messenger> messengers = new ArrayList<>();
}
