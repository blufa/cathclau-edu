package gn.lacaeducation.students.application.event;

/**
 * Publié quand une inscription est validée (ou transférée) dans une classe déjà à sa capacité
 * maximale, malgré l'avertissement, parce qu'un utilisateur autorisé a explicitement demandé à
 * forcer le dépassement (§6 cahier des charges — ne jamais augmenter la capacité silencieusement).
 * Consumed by the audit trail (AuditEventListener) pour garder une trace de qui a forcé quoi.
 */
public record ClassCapacityOverriddenEvent(Long classId, long countBeforeOverride, Integer maxCapacity,
                                            Long overriddenByUserId) {}
