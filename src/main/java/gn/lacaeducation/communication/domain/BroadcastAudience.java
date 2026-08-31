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
    /** Parent(s) d'un élève précis (ex. notification ponctuelle retard/absence). */
    STUDENT_PARENTS
}
