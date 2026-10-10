package gn.lacaeducation.transport.adapters.out.persistence;

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

/** Vue en lecture seule de la table "invoices" (possédée par billing) pour le module transport —
 * seulement les colonnes nécessaires à {@link TransportInvoiceQueryAdapter}. */
@Entity
@Table(name = "invoices")
@Getter @Setter @NoArgsConstructor
@org.hibernate.annotations.Filter(name = "tenantFilter")
class TransportInvoiceMirrorEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;
    @Column(name = "student_id", nullable = false)
    private Long studentId;
    @Column(name = "fee_id")
    private Long feeId;
    @Column(name = "paid_amount", nullable = false)
    private BigDecimal paidAmount;
    @Column(name = "installment_id")
    private Long installmentId;
}
