package gn.lacaeducation.billing;

import java.math.BigDecimal;

/**
 * API publique étroite exposée par {@code billing} au module {@code courses} : un cours (vidéo/PDF
 * payant, §16 B) réutilise entièrement la facturation scolaire — frais miroir (type OTHER) + facture
 * normale — sans jamais accéder aux ports internes ({@code SchoolFeeUseCase}, {@code InvoiceUseCase})
 * ni aux types de domaine de {@code billing}, qui restent encapsulés.
 */
public interface CourseBillingApi {

    /** Crée le frais miroir (type OTHER) d'un nouveau cours ; renvoie son identifiant. */
    Long createCourseFee(Long tenantId, Long classId, BigDecimal amount, String academicYear, String label);

    /** Met à jour uniquement le montant/libellé du frais miroir d'un cours (conserve classe/type/année). */
    void updateCourseFeeAmount(Long feeId, BigDecimal amount, String label);

    /** Émet (référence auto-générée) la facture d'achat d'un cours pour un élève ; renvoie son identifiant. */
    Long issueCourseInvoice(Long tenantId, Long studentId, Long feeId, Long academicYearId, BigDecimal amount);
}
