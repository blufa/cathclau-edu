package gn.lacaeducation.billing.application.port.in;

import gn.lacaeducation.billing.domain.StudentCredit;
import gn.lacaeducation.billing.domain.StudentCreditUsage;

import java.math.BigDecimal;
import java.util.List;

/**
 * Trop-perçu conservé comme crédit disponible de l'élève (§4 cahier des charges). Créé automatiquement
 * par {@code InvoiceService#applyPayment} quand un encaissement dépasse le solde dû d'une facture ;
 * applicable ensuite à n'importe quelle facture future du MÊME élève via {@link #applyToInvoice}.
 */
public interface StudentCreditUseCase {

    /** Crée un nouveau trop-perçu pour un élève — appelé en interne par le module billing, jamais
     * directement depuis un contrôleur (pas de "créer du crédit from scratch" côté UI, uniquement en
     * sous-produit d'un encaissement réel). */
    StudentCredit create(Long tenantId, Long studentId, BigDecimal amount, Long sourcePaymentId,
                        Long sourceInvoiceId, String reason);

    /** Somme des crédits encore disponibles (non épuisés) de cet élève. */
    BigDecimal availableBalance(Long studentId);

    /** Détail des lots de crédit de cet élève (épuisés inclus, pour l'historique). */
    List<StudentCredit> findByStudentId(Long studentId);

    /** Historique des applications (facture cible, montant, auteur, date), le plus récent en premier. */
    List<StudentCreditUsage> findUsageHistory(Long studentId);

    /**
     * Applique jusqu'à {@code amount} de crédit disponible de cet élève à UNE facture précise (jamais
     * celle d'un autre élève, §4 : "ne jamais utiliser le crédit d'un élève pour un autre élève"),
     * du plus ancien lot au plus récent (FIFO). Lève une exception métier si le crédit disponible est
     * insuffisant plutôt que d'appliquer un montant partiel silencieux. Verrouille chaque lot utilisé
     * le temps de la transaction pour qu'un encaissement concurrent ne puisse pas consommer le même
     * crédit deux fois (§4 : "appels simultanés").
     *
     * @return le montant réellement appliqué (= {@code amount} si le crédit disponible suffisait).
     */
    BigDecimal applyToInvoice(Long studentId, Long invoiceId, BigDecimal amount, Long appliedByUserId);
}
