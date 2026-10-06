package gn.lacaeducation.billing.application.event;

import java.math.BigDecimal;

/** Publié à la suppression d'une échéance/tranche (InstallmentService#delete, et en cascade par
 * SchoolFeeService#delete quand le frais lui-même est supprimé — {@code deletedBy} est alors null,
 * déjà couvert par le {@link SchoolFeeDeletedEvent} correspondant). Consommé par le journal d'audit
 * (§retour fondateurs) : même raisonnement que {@link SchoolFeeDeletedEvent}. */
public record InstallmentDeletedEvent(Long tenantId, Long installmentId, Long feeId, String label,
                                      BigDecimal amount, Long deletedBy) {}
