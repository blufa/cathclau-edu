package gn.lacaeducation.billing.application.event;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Publié à la création d'une échéance/tranche (InstallmentService#create et #generateTerms).
 * Consommé par le journal d'audit (§retour fondateurs), même raisonnement que
 * {@link SchoolFeeCreatedEvent} : trace qui a créé quelle tranche, pour quel frais, à quel montant. */
public record InstallmentCreatedEvent(Long tenantId, Long installmentId, Long feeId, String label,
                                      BigDecimal amount, LocalDate dueDate, Long createdBy) {}
