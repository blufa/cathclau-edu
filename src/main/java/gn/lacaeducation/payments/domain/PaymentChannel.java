package gn.lacaeducation.payments.domain;
public enum PaymentChannel {
    ORANGE_MONEY, MTN_MONEY, WAVE, BANK_CARD, DJOMY, CASH,
    /** Retenue sur salaire (§payroll ↔ billing) : l'argent n'est jamais physiquement encaissé au
     * guichet, il reste dû par l'école au salarié jusqu'à la paie — jamais {@link #isDeskManual()}
     * (pas de session de caisse à rapprocher), toujours créé déjà {@code SUCCEEDED} par {@code
     * PaymentUseCase#recordSystemPayment}, uniquement pour produire un reçu de paiement au parent. */
    PAYROLL_DEDUCTION;

    /**
     * Canaux encaissés manuellement au guichet : l'argent est déjà reçu (espèces, Orange Money,
     * virement bancaire), on enregistre le paiement immédiatement sans passerelle en ligne.
     * Les paiements en ligne du parent depuis chez lui passent par DJOMY (redirection portail).
     */
    public boolean isDeskManual() {
        return this == CASH || this == ORANGE_MONEY || this == BANK_CARD;
    }
}
