package gn.lacaeducation.expenses.domain;

/**
 * Mode de financement d'un budget (notamment un budget d'événement : kermesse, sortie…) quand son
 * montant ne peut pas être couvert par la trésorerie courante de l'école.
 */
public enum FinancingMode {
    /** Financé par la trésorerie courante, sans collecte ni prêt. */
    NONE,
    /** Collecte de fonds auprès des parents/personnel pour financer l'événement. */
    COLLECTE,
    /** Prêt/avance de trésorerie auprès de la comptabilité, à rembourser. */
    PRET
}
