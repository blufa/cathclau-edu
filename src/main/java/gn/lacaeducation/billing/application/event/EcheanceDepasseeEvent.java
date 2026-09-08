package gn.lacaeducation.billing.application.event;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Un événement par élève dont la facture rattachée à l'échéance {@code installmentId} n'est pas soldée
 * (§8.2) — pas un événement par échéance : le module {@code communication} n'a pas le droit de
 * dépendre de l'API de requête de {@code billing} (seuls ses événements sont une cible autorisée, voir
 * ModularityTests), donc c'est {@code billing} lui-même (qui a accès à son propre
 * {@code InvoiceRepository}) qui résout les élèves concernés avant de publier.
 */
public record EcheanceDepasseeEvent(Long tenantId, Long studentId, Long installmentId, Long feeId,
                                     String invoiceReference, BigDecimal amount, LocalDate dueDate) {}
