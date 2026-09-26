package gn.lacaeducation.billing.application.port.in;

import gn.lacaeducation.billing.domain.Invoice;
import gn.lacaeducation.billing.domain.SchoolFee;
import gn.lacaeducation.students.EnrollmentType;

import java.util.List;

/**
 * Génère en un seul appel toutes les factures des frais rattachés à la classe d'un élève, à
 * l'inscription (§retour comptable, refonte inscription §5.2) — pour que le comptable n'ait plus à
 * ouvrir un écran séparé par frais (inscription, mensualité, cantine, examen...). Ne réécrit AUCUN
 * calcul de remise : délègue systématiquement à {@link InvoiceUseCase#create} /
 * {@link InvoiceUseCase#generateMonthly}, qui appliquent déjà exonération/fratrie/personnel de façon
 * transparente.
 */
public interface EnrollmentBillingUseCase {

    /**
     * @param enrollmentType  NEW ou RE_ENROLLMENT — exclut le frais du type opposé (ENROLLMENT vs
     *                        RE_ENROLLMENT) si la classe a les deux configurés, pour ne jamais générer
     *                        la mauvaise facture d'inscription.
     * @param transportFeeId  frais TRANSPORT choisi (rattaché à une ligne de bus, pas à la classe —
     *                        jamais résolu automatiquement via {@code classId}) ; ignoré si
     *                        {@code wantsTransport} est faux.
     */
    record GenerateAllFeesCommand(Long tenantId, Long studentId, Long classId, Long academicYearId,
                                  EnrollmentType enrollmentType, boolean wantsTransport, Long transportFeeId) {}

    /** Les factures effectivement créées pour ce frais — vide si une facture existait déjà (idempotent). */
    record GeneratedFeeInvoices(SchoolFee fee, List<Invoice> invoices) {}

    List<GeneratedFeeInvoices> generateAllForEnrollment(GenerateAllFeesCommand cmd);
}
