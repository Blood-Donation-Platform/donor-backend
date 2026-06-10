package pt.sanguept.location.entities;

import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Point;
import pt.sanguept.commoninfra.entities.BaseEntity;
import pt.sanguept.territory.entities.AdministrativeDivision;

import java.util.UUID;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Location extends BaseEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String address;

    @Column(columnDefinition = "geometry", nullable = false)
    private Point coordinates;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private AdministrativeDivision administrativeDivision;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    private String externalId;

}
