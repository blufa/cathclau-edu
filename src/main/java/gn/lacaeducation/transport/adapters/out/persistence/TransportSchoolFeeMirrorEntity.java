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

/** Vue en lecture seule de la table "school_fees" (possédée par billing) pour le module transport —
 * sert uniquement à retrouver les frais de type TRANSPORT (voir {@link TransportInvoiceQueryAdapter}). */
@Entity
@Table(name = "school_fees")
@Getter @Setter @NoArgsConstructor
@org.hibernate.annotations.Filter(name = "tenantFilter")
class TransportSchoolFeeMirrorEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;
    @Column(nullable = false, length = 20)
    private String type;
}
