package gn.lacaeducation.academics.domain;

/** Jour férié (chômé et payé, décidé par l'État) : les enseignants concernés restent TOUJOURS payés
 *  sur les heures touchées, sans choix de la direction. Autre événement école (kermesse, sortie,
 *  entraînement... décidé par l'école) : la direction choisit explicitement quels enseignants sont
 *  considérés comme ayant fait cours (voir {@code SchoolEventCreditedTeacher}). */
public enum SchoolEventType {
    HOLIDAY, OTHER
}
