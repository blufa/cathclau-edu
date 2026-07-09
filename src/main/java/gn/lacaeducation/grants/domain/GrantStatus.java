package gn.lacaeducation.grants.domain;

/** Cycle de vie d'une subvention : attendue → partiellement reçue → reçue → annulée. */
public enum GrantStatus { EXPECTED, PARTIAL, RECEIVED, CANCELLED }
