package gn.lacaeducation.payroll.domain;

/** Statut d'une avance sur salaire : en cours de remboursement, soldée (déduite en totalité) ou
 * annulée (avant toute déduction). */
public enum SalaryAdvanceStatus {
    ACTIVE,
    REPAID,
    CANCELLED
}
