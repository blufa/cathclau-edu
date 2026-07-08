package gn.lacaeducation.billing.domain;

/**
 * Étape de recouvrement d'une facture impayée — contexte écoles privées en Guinée : une ou deux
 * relances amiables, puis exclusion de l'élève (accès bloqué jusqu'à régularisation).
 */
public enum RecoveryStage { NONE, REMINDER_1, REMINDER_2, EXCLUDED }
