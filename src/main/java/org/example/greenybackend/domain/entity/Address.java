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
@Table(name = "ADDRESS")
public class Address {

    @Id
    @EqualsAndHashCode.Include
    @Column(name = "ADDRESS_ID", nullable = false)
    private String addressId;

    @Column(name = "RECEIVER_NAME", length = 100)
    private String receiverName;

    @Column(name = "RECEIVER_PHONE", length = 11)
    private String receiverPhone;

    @Column(name = "ADDRESS_DETAIL", length = 50)
    private String addressDetail;

    @Column(name = "WARD_NAME", length = 100)
    private String wardName;

    @Column(name = "DISTRICT_NAME", length = 100)
    private String districtName;

    @Column(name = "PROVINCE_NAME", length = 100)
    private String provinceName;

    @Column(name = "TYPE")
    private Integer type;

    @Column(name = "IS_DEFAULT")
    private Boolean isDefault;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", nullable = false)
    private UserEntity userEntity;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "address")
    @Builder.Default
    private List<Orders> ordersList = new ArrayList<>();

}
