package gn.lacaeducation.students.domain;

/** ABANDONED : l'élève a bien été inscrit et a suivi les cours un temps avant d'arrêter en cours
 * d'année — distinct de CANCELLED (jamais vraiment commencé / erreur administrative), qui exclut
 * l'élève de la plupart des calculs. Un abandon reste visible en facturation/recouvrement (les
 * factures déjà émises restent dues), mais disparaît des listes actives du quotidien (appel,
 * saisie de notes) et de l'effectif de classe. */
public enum EnrollmentStatus { PENDING, VALIDATED, CANCELLED, ABANDONED }
