package gn.lacaeducation.dpe.domain;

/** Types d'alerte évalués automatiquement (première tranche : incohérences d'effectifs, dérivées des
 * mêmes données que le tableau de bord — voir DpeAlertService). D'autres types (taux d'absence,
 * synchronisation) suivront une fois les agrégations correspondantes construites. */
public enum DpeAlertType {
    SCHOOL_NO_STUDENTS,
    SCHOOL_NO_TEACHERS,
    SCHOOL_NO_CLASSES,
    SCHOOL_INACTIVE,
    HIGH_STUDENT_TEACHER_RATIO
}
