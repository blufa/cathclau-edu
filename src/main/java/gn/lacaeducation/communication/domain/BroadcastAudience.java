package gn.lacaeducation.communication.domain;

/** Segmentation des destinataires d'une diffusion de masse (§11.1). */
public enum BroadcastAudience {
    /** Tous les parents du tenant. */
    ALL_PARENTS,
    /** Parents des élèves d'une classe donnée. */
    CLASS_PARENTS,
    /** Parents des élèves masculins d'une classe donnée. */
    CLASS_PARENTS_BOYS,
    /** Parents des élèves féminins d'une classe donnée. */
    CLASS_PARENTS_GIRLS,
    /** Tous les enseignants du tenant. */
    ALL_TEACHERS,
    /** Tout le personnel du tenant : enseignants ET personnel non-enseignant (ex. annonce de paie). */
    ALL_STAFF,
    /** Parent(s) d'un élève précis (ex. notification ponctuelle retard/absence). */
    STUDENT_PARENTS,
    /** Toute l'école : parents (élèves), enseignants ET personnel non-enseignant — ex. annonce d'un
     * événement école à tout le monde. */
    ALL,
    /** Parents des élèves ayant au moins une échéance de paiement dépassée (retard) — relance SMS
     * groupée, sans avoir à composer la liste manuellement (voir PaymentReminderCandidatesService). */
    OVERDUE_PAYERS,
    /** Parents des élèves ayant une échéance de paiement qui approche (pas encore en retard) — relance
     * préventive (voir PaymentReminderCandidatesService). */
    UPCOMING_PAYERS
}
