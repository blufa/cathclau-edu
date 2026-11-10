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

/** Vue en lecture seule de la table "installments" (possédée par billing) pour le module transport,
 * à déterminer si une facture transport payée couvre un mois donné (§retour comptable : "qui a payé
 * le mois en cours"). {@code months} (ex. "2026-10", plusieurs mois séparés par des virgules pour une
 * tranche multi-mois) porte le(s) mois RÉELLEMENT couverts — {@code dueDate} n'est qu'une échéance de
 * paiement (le 5 du mois SUIVANT par convention), jamais le mois couvert lui-même (§retour utilisateur :
 * "si tu filtres par mois rien ne vient", filtrer sur dueDate décalait tout d'un mois). Pas de
 * tenant_id : filtrage via la facture liée. */
@Entity
@Table(name = "installments")
@Getter @Setter @NoArgsConstructor
class TransportInstallmentMirrorEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;
    @Column(name = "months")
    private String months;
}
