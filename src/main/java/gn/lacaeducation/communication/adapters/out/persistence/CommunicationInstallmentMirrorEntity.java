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

import java.time.LocalDate;

/** Vue en lecture seule de la table "installments" (possédée par billing) pour le module
 * communication. Pas de tenant_id : cette table n'en a pas directement (le tenant se lit via
 * school_fees), le filtrage se fait via la facture liée ou via {@code feeId}. */
@Entity
@Table(name = "installments")
@Getter @Setter @NoArgsConstructor
class CommunicationInstallmentMirrorEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "fee_id", nullable = false)
    private Long feeId;
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;
}
