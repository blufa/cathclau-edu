package gn.lacaeducation.common;

/**
 * Thrown when validating/transférant une inscription ferait dépasser la capacité maximale d'une
 * classe (§6 cahier des charges) — mappée en 409 avec {classId, currentCount, maxCapacity,
 * raison:"CAPACITE_ATTEINTE"} pour que le frontend distingue ce conflit précis d'un conflit générique
 * et propose un bouton "Forcer malgré tout" à un utilisateur autorisé (le second appel passe
 * overrideCapacity=true, qui contourne ce contrôle sans jamais l'augmenter silencieusement).
 */
public class ClassCapacityExceededException extends RuntimeException {
    private final Long classId;
    private final long currentCount;
    private final Integer maxCapacity;

    public ClassCapacityExceededException(Long classId, long currentCount, Integer maxCapacity) {
        super("La classe " + classId + " a atteint sa capacité maximale (" + currentCount + "/" + maxCapacity + ").");
        this.classId = classId;
        this.currentCount = currentCount;
        this.maxCapacity = maxCapacity;
    }

    public Long getClassId() { return classId; }
    public long getCurrentCount() { return currentCount; }
    public Integer getMaxCapacity() { return maxCapacity; }
}
