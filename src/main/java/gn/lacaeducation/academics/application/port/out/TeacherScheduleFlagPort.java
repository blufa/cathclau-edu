package gn.lacaeducation.academics.application.port.out;

/**
 * Lit un attribut du personnel enseignant (module staff) pertinent pour l'emploi du temps, sans
 * dépendance Java vers ce module — voir {@code TeacherFlagJpaEntity} (lecture directe de la table
 * partagée {@code teachers}, comme {@code EnrollmentQueryPort} le fait déjà pour {@code enrollments}).
 */
public interface TeacherScheduleFlagPort {
    /** Vrai si cet enseignant peut couvrir plusieurs classes en même temps (ex. professeur d'EPS qui
     * rassemble plusieurs classes ensemble sur le même créneau, §5.6) — désactive la détection de
     * conflit d'emploi du temps sur cet enseignant précis (jamais les conflits de salle/classe). Faux
     * si l'enseignant est introuvable. */
    boolean allowsConcurrentClasses(Long teacherId);
}
