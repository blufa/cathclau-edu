package gn.lacaeducation.expenses.domain;

/**
 * Cycle de vie d'un budget annuel : le comptable rédige (DRAFT) puis soumet (SUBMITTED) ;
 * la direction valide (APPROVED) ou rejette (REJECTED). Un budget approuvé peut être clôturé.
 */
public enum BudgetStatus { DRAFT, SUBMITTED, APPROVED, REJECTED, CLOSED }
