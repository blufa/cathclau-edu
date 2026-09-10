package gn.lacaeducation.billing.domain;
/** CANCELLED : facture annulée sans effet financier (solde remis à zéro) mais conservée pour
 * l'historique — distinct de la suppression physique (voir InvoiceUseCase#cancel). Utilisé notamment
 * par le transfert de classe en cours d'année, quand les mensualités de l'ancienne classe ne
 * s'appliquent plus. */
public enum InvoiceStatus { UNPAID, PARTIAL, PAID, CANCELLED }
