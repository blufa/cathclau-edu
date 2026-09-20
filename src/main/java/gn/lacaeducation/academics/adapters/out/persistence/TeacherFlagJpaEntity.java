package gn.lacaeducation.academics.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Vue en lecture seule de la table {@code teachers} (propriété du module staff), utilisée uniquement
 * par le module academics pour vérifier si un enseignant peut couvrir plusieurs classes en même temps
 * (§5.6) — même pattern que {@code BillingEnrollmentJpaEntity} (module billing) : entité dédiée sur la
 * table partagée, pour éviter toute dépendance Java vers le module staff (qui dépend déjà lui-même
 * d'academics — une dépendance dans l'autre sens créerait un cycle entre les deux modules).
 */
@Entity
@Table(name = "teachers")
@Getter @Setter @NoArgsConstructor
class TeacherFlagJpaEntity {
    @Id private Long id;
    @Column(name = "allows_concurrent_classes", nullable = false) private boolean allowsConcurrentClasses;
}
