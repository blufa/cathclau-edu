package gn.lacaeducation.billing.application.event;

import java.math.BigDecimal;

/** Published when an exemption is granted (ExemptionService#grant). Consumed by the audit trail
 * (§retour fondateurs — contrôle anti-fraude) : trace qui a accordé quelle remise, à qui et pourquoi. */
public record ExemptionGrantedEvent(Long tenantId, Long exemptionId, Long studentId, String type,
                                    BigDecimal percentage, BigDecimal amount, String reason,
                                    Long validatedBy) {}
