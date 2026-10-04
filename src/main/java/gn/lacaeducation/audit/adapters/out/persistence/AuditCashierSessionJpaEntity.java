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

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Vue en lecture seule de la table cashier_sessions, utilisée exclusivement par le module audit
 * pour le détecteur B3 (écarts statistiques entre comptables), sans dépendre des beans de
 * {@code payments}.
 */
@Entity
@Table(name = "cashier_sessions")
@Getter @Setter @NoArgsConstructor
class AuditCashierSessionJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;
    @Column(name = "cashier_id", nullable = false)
    private Long cashierId;
    @Column(name = "closed_at")
    private LocalDateTime closedAt;
    @Column(precision = 15, scale = 2)
    private BigDecimal discrepancy;
    @Column(nullable = false, length = 20)
    private String status;
}
