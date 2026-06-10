package pt.sanguept.donationsession.entities;

import jakarta.persistence.*;
import lombok.*;
import pt.sanguept.commoninfra.entities.AuditedEntity;
import pt.sanguept.donationsession.enums.SessionStatus;
import pt.sanguept.donationlocation.entities.Location;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class DonationSession extends AuditedEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String title;

    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Location location;

    @Column(nullable = false)
    private LocalDateTime startAt;

    @Column(nullable = false)
    private LocalDateTime endAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionStatus sessionStatus;

}
