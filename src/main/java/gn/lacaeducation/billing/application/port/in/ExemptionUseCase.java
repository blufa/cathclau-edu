package gn.lacaeducation.billing.application.port.in;

import gn.lacaeducation.billing.domain.Exemption;
import gn.lacaeducation.billing.domain.ExemptionType;
import gn.lacaeducation.billing.domain.FeeType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface ExemptionUseCase {
    record GrantExemptionCommand(Long studentId, ExemptionType type, BigDecimal percentage, BigDecimal amount,
                                 FeeType feeType, String reason, LocalDate startDate, LocalDate endDate) {}

    /** Accorde une exonération (traçée : responsable = {@code validatedBy}). */
    Exemption grant(Long tenantId, GrantExemptionCommand command, Long validatedBy);

    /** Révoque une exonération (archivée : {@code active = false}, jamais supprimée). */
    Exemption revoke(Long id);

    List<Exemption> findByTenant(Long tenantId);
    List<Exemption> findByStudent(Long studentId);
}
