package gn.lacaeducation.billing.application.port.in;

import gn.lacaeducation.billing.domain.Exemption;
import gn.lacaeducation.billing.domain.ExemptionType;
import gn.lacaeducation.billing.domain.FeeType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface ExemptionUseCase {
    /** {@code feeTypes} : un seul type, plusieurs, ou vide/null = tous les frais. */
    record GrantExemptionCommand(Long studentId, ExemptionType type, BigDecimal percentage, BigDecimal amount,
                                 List<FeeType> feeTypes, String reason, LocalDate startDate, LocalDate endDate) {}

    /** Accorde une exonération (traçée : responsable = {@code validatedBy}). */
    Exemption grant(Long tenantId, GrantExemptionCommand command, Long validatedBy);

    /** Révoque une exonération (archivée : {@code active = false}, jamais supprimée). */
    Exemption revoke(Long id);

    List<Exemption> findByTenant(Long tenantId);
    List<Exemption> findByStudent(Long studentId);

    /** Exonération applicable à un élève pour un frais donné, parmi les exonérations actives et dans
     * leur fenêtre de dates (§8). {@code percentage} = le meilleur taux parmi les exonérations
     * TOTAL/PARTIAL applicables (jamais cumulées entre elles) ; {@code exceptionalAmount} = la somme
     * des remises EXCEPTIONAL applicables (montants fixes, décisions ponctuelles documentées —
     * cumulables entre elles et avec le pourcentage). */
    record ApplicableExemption(BigDecimal percentage, String percentageReason,
                               BigDecimal exceptionalAmount, String exceptionalReason) {}

    ApplicableExemption computeApplicable(Long studentId, FeeType feeType);
}
