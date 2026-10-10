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

import java.time.LocalDate;

/** Vue en lecture seule de la table "installments" (possédée par billing) pour le module transport —
 * seule {@code dueDate} sert, à déterminer si une facture transport payée couvre le mois en cours
 * (§retour comptable : "qui a payé le mois en cours"). Pas de tenant_id : filtrage via la facture liée. */
@Entity
@Table(name = "installments")
@Getter @Setter @NoArgsConstructor
class TransportInstallmentMirrorEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;
}
