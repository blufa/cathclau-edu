package gn.lacaeducation.students;

/**
 * API publique du module students pour le cloisonnement du portail parent :
 * permet de vérifier qu'un compte parent est bien rattaché à un élève avant de lui
 * exposer des données (notes, bulletins, présences…). Lecture seule.
 */
public interface ParentAccessProvider {

    /** Vrai si le compte utilisateur {@code userId} est un parent rattaché à l'élève {@code studentId}. */
    boolean isParentOfStudent(Long userId, Long studentId);

    /** Identifiants des élèves rattachés au compte parent {@code userId} (vide si non parent). */
    java.util.List<Long> findChildStudentIds(Long userId);

    /** Identifiants des comptes utilisateurs des parents d'un élève (pour notifier ; vide si aucun). */
    java.util.List<Long> findParentUserIdsByStudent(Long studentId);
}
