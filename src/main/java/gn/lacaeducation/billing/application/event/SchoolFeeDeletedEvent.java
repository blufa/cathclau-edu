package gn.lacaeducation.billing.application.event;

import java.math.BigDecimal;

/** Publié à la suppression d'un frais scolaire (SchoolFeeService#delete — refusée si des factures
 * existent déjà pour ce frais, voir sa Javadoc). Consommé par le journal d'audit (§retour
 * fondateurs) : la suppression d'un frais encore non facturé (ex. "tranches" créées la veille, pas
 * encore encaissées) est la seule façon légitime, via l'application, de faire disparaître un frais —
 * cette trace permet de distinguer une suppression faite dans l'écran "Frais scolaires" d'une
 * disparition inexpliquée (manipulation directe de la base, hors de l'application). */
public record SchoolFeeDeletedEvent(Long tenantId, Long feeId, String label, String type,
                                    BigDecimal amount, String academicYear, Long deletedBy) {}
