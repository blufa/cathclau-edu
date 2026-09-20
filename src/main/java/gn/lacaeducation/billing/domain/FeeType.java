package gn.lacaeducation.billing.domain;
/** OTHER : catégorie de frais annexe créée à la volée par l'école (tenue scolaire, fournitures,
 * bus...), nommée via {@link SchoolFee#getLabel()}.
 * PLATFORM : frais annexe « usage de la plateforme » — chaque école le configure elle-même (nom et
 * montant, via {@link SchoolFee#getLabel()} comme OTHER), mais tout encaissement sur une facture de ce
 * type alimente en plus le solde dû à l'éditeur de la plateforme (voir
 * {@code platform.PlatformFeeLedgerApi}, appelé depuis {@code billing.InvoiceService#applyPayment}). */
public enum FeeType { ENROLLMENT, RE_ENROLLMENT, MONTHLY, TRANSPORT, CANTEEN, EXAM, OTHER, PLATFORM }
