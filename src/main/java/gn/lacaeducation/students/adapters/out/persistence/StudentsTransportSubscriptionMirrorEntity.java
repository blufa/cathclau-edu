package gn.lacaeducation.students.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Vue en lecture seule de la table "transport_subscriptions" (possédée par transport) pour le
 * module students — sert uniquement au filtre "Abonnés au transport" de la recherche élèves. */
@Entity
@Table(name = "transport_subscriptions")
@Getter @Setter @NoArgsConstructor
@org.hibernate.annotations.Filter(name = "tenantFilter")
class StudentsTransportSubscriptionMirrorEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;
    @Column(name = "student_id", nullable = false)
    private Long studentId;
    @Column(nullable = false, length = 16)
    private String status;
}
