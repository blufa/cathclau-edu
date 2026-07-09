package gn.lacaeducation.payments.domain;
public enum PaymentChannel {
    ORANGE_MONEY, MTN_MONEY, WAVE, BANK_CARD, DJOMY, CASH;

    /**
     * Canaux encaissés manuellement au guichet : l'argent est déjà reçu (espèces, Orange Money,
     * virement bancaire), on enregistre le paiement immédiatement sans passerelle en ligne.
     * Les paiements en ligne du parent depuis chez lui passent par DJOMY (redirection portail).
     */
    public boolean isDeskManual() {
        return this == CASH || this == ORANGE_MONEY || this == BANK_CARD;
    }
}
