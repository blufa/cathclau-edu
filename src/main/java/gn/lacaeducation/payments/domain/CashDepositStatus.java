package gn.lacaeducation.payments.domain;

/** Cycle de vie d'un dépôt de fonds comptable → fondateur (§5 cahier des charges). PENDING jusqu'à la
 * décision du fondateur ; jamais réévalué après VALIDATED/REJECTED (transition à sens unique). */
public enum CashDepositStatus {
    PENDING, VALIDATED, REJECTED
}
