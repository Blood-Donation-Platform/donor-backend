package pt.sanguept.identity.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false, unique = true)
    private String tokenHash;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Instant expiresAt;

    @Builder.Default
    private Instant createdAt = Instant.now();

    @Builder.Default
    private boolean revoked = false;

    private Long replacedByTokenId;

    private String deviceId;

    private Instant usedAt;

}
