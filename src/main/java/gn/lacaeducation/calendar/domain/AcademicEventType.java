package gn.lacaeducation.calendar.domain;

/** Types d'événements du calendrier, adaptés à une école privée secondaire (collège/lycée). */
public enum AcademicEventType {
    HOLIDAY,        // jour férié
    VACATION,       // vacances scolaires
    EXAM,           // composition / examen
    MEETING,        // réunion de parents / conseil de classe
    FEE_DUE,        // échéance de paiement
    RESULT,         // proclamation des résultats
    ENROLLMENT,     // rentrée / période d'inscription
    OTHER
}
