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
}
