package gn.lacaeducation.audit.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Vue en lecture seule de la table exemptions, utilisée exclusivement par le module audit pour le
 * détecteur B3, sans dépendre des beans de {@code billing}.
 */
@Entity
@Table(name = "exemptions")
@Getter @Setter @NoArgsConstructor
class AuditExemptionJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;
    @Column(name = "validated_by")
    private Long validatedBy;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
