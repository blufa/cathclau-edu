package gn.lacaeducation.billing.application.event;

import java.math.BigDecimal;

/** Publié à la création d'un frais scolaire (SchoolFeeService#create). Consommé par le journal
 * d'audit (§retour fondateurs) : un comptable qui signale "mes frais ont disparu" doit pouvoir
 * vérifier dans /audit-logs qu'ils ont bien été créés, quand, et par qui — {@code createdBy} est
 * null pour un frais généré automatiquement par le système (ex. frais de transport/cours, voir
 * CourseBillingApiImpl), jamais pour une création depuis l'écran "Frais scolaires". */
public record SchoolFeeCreatedEvent(Long tenantId, Long feeId, String label, String type,
                                    BigDecimal amount, String academicYear, Long createdBy) {}
