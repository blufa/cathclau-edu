package gn.lacaeducation.billing.domain;
/** OTHER : catégorie de frais annexe créée à la volée par l'école (tenue scolaire, fournitures,
 * bus...), nommée via {@link SchoolFee#getLabel()}. */
public enum FeeType { ENROLLMENT, RE_ENROLLMENT, MONTHLY, TRANSPORT, CANTEEN, EXAM, OTHER }
