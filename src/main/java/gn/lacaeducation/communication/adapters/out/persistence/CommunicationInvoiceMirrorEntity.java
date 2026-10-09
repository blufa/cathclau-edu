package gn.lacaeducation.communication.adapters.out.persistence;

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

/** Vue en lecture seule de la table "invoices" (possédée par billing) pour le module communication —
 * sert uniquement à {@link PaymentReminderDataAdapter} (relance SMS retard/échéance proche). */
@Entity
@Table(name = "invoices")
@Getter @Setter @NoArgsConstructor
@org.hibernate.annotations.Filter(name = "tenantFilter")
class CommunicationInvoiceMirrorEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;
    @Column(name = "student_id", nullable = false)
    private Long studentId;
    @Column(name = "fee_id")
    private Long feeId;
    @Column(name = "installment_id")
    private Long installmentId;
    @Column(name = "remaining_balance", nullable = false)
    private BigDecimal remainingBalance;
    /** Chaîne brute ("UNPAID"/"PARTIAL"/"PAID"/"CANCELLED") — jamais l'enum de billing, qui
     * appartiendrait à l'autre module. */
    @Column(nullable = false)
    private String status;
}
