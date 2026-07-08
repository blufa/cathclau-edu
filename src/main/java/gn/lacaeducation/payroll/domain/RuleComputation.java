package gn.lacaeducation.payroll.domain;

/** Mode de calcul d'une règle de paie : montant fixe, % du salaire de base, ou % du brut. */
public enum RuleComputation { FIXED, PERCENT_OF_BASE, PERCENT_OF_GROSS }
