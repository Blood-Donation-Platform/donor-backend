package pt.sanguept.territory.entities;

import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Geometry;
import pt.sanguept.commoninfra.entities.BaseEntity;

import java.util.UUID;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class AdministrativeDivision extends BaseEntity {

    @Id
    @GeneratedValue
    private UUID id;

    private String name;

    @Column(columnDefinition = "geometry")
    private Geometry geometry;

    @ManyToOne(fetch = FetchType.LAZY)
    private AdministrativeDivision parent;

}
