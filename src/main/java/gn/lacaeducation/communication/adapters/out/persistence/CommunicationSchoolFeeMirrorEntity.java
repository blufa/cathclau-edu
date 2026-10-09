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

/** Vue en lecture seule de la table "school_fees" (possédée par billing) pour le module
 * communication — sert uniquement à retrouver la classe d'un frais (voir {@link PaymentReminderDataAdapter}). */
@Entity
@Table(name = "school_fees")
@Getter @Setter @NoArgsConstructor
@org.hibernate.annotations.Filter(name = "tenantFilter")
class CommunicationSchoolFeeMirrorEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;
    @Column(name = "class_id")
    private Long classId;
    /** Chaîne brute ("MONTHLY"/"TRANSPORT"/...) — jamais l'enum de billing. */
    @Column(nullable = false)
    private String type;
}
