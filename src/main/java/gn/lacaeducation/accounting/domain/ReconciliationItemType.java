package gn.lacaeducation.accounting.domain;

/**
 * Éléments en suspens d'un rapprochement. Côté banque : DEPOSIT_IN_TRANSIT (+), OUTSTANDING_CHEQUE (−).
 * Côté livre : BANK_INTEREST (+), BANK_CHARGE (−). ERROR/OTHER ajustent le livre du montant signé.
 */
public enum ReconciliationItemType { OUTSTANDING_CHEQUE, DEPOSIT_IN_TRANSIT, BANK_CHARGE, BANK_INTEREST, ERROR, OTHER }
