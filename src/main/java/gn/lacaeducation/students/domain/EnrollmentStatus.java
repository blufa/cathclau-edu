package gn.lacaeducation.students.domain;

/** ABANDONED : l'élève a bien été inscrit et a suivi les cours un temps avant de partir en cours
 * d'année (transfert vers une autre école, abandon, exclusion...) — distinct de CANCELLED (jamais
 * vraiment commencé / erreur administrative), qui exclut l'élève de la plupart des calculs. Un
 * départ disparaît des listes actives du quotidien (appel, saisie de notes) et de l'effectif de
 * classe ; ses factures encore dues de l'année sont annulées (voir
 * {@code EnrollmentAbandonedInvoiceListener}), mais l'historique déjà encaissé reste intact. */
public enum EnrollmentStatus { PENDING, VALIDATED, CANCELLED, ABANDONED }
