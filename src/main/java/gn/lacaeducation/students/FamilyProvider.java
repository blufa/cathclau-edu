package gn.lacaeducation.students;

import java.time.LocalDate;
import java.util.List;

/**
 * API publique du module students pour les règles familiales (fratrie) : les enfants rattachés aux
 * mêmes parents qu'un élève donné (lui inclus), avec leur date de naissance pour identifier le plus jeune.
 */
public interface FamilyProvider {
    record ChildInfo(Long studentId, LocalDate birthDate) {}

    /** Tous les enfants de la même famille que {@code studentId} (l'élève lui-même inclus). */
    List<ChildInfo> findSiblings(Long studentId);
}
