package gn.lacaeducation.students;

/**
 * Outbound port (part of the students public API) letting students read a class's maximum capacity
 * (owned by academics) to enforce the capacity rule at enrollment validation. The academics module
 * provides the adapter, so the only static dependency is academics -> students, matching the existing
 * enrollment-event listener and avoiding a module cycle.
 */
public interface ClassCapacityPort {
    /** Maximum capacity of the class, or {@code null} if unlimited/unknown. */
    Integer maxCapacity(Long classId);

    /** Même chose, mais pose d'abord un verrou pessimiste sur la ligne de la classe (dans la
     * transaction appelante) — à utiliser juste avant de compter les inscriptions déjà validées et
     * d'en valider une nouvelle, pour que deux validations concurrentes sur la même classe ne
     * puissent jamais dépasser la capacité ensemble (§6 cahier des charges). */
    Integer lockAndGetMaxCapacity(Long classId);
}
