package pt.sanguept.donationnotification.entities;

import jakarta.persistence.*;
import lombok.*;
import pt.sanguept.commoninfra.entities.BaseEntity;
import pt.sanguept.donationnotification.enums.SubscriptionType;

import java.util.UUID;

@Entity
@Table(name = "notification_subscription")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class NotificationSubscription extends BaseEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionType type;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    @Column(name = "administrative_division_id")
    private UUID administrativeDivisionId;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "radius_km")
    private Integer radiusKm;

}
