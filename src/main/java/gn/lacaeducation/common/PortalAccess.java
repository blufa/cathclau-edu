package gn.lacaeducation.common;

import org.springframework.security.access.AccessDeniedException;

import java.util.Collection;
import java.util.Set;
import java.util.function.BiPredicate;

/**
 * Cloisonnement du portail parent/élève : un compte parent (sans rôle d'encadrement) ne peut accéder
 * qu'aux élèves qui lui sont rattachés. Centralisé ici pour être réutilisé par tous les modules
 * exposant des données par élève, sans coupler ces modules entre eux (le lien parent↔élève est
 * fourni via un prédicat).
 */
public final class PortalAccess {

    /** Rôles du personnel encadrant : accès complet aux données d'un élève. */
    private static final Set<String> STAFF_ROLES = Set.of(
            "SUPER_ADMIN", "SCHOOL_ADMIN", "DIRECTOR", "RESPONSABLE_PEDAGOGIQUE",
            "TEACHER", "SECRETARY", "ACCOUNTANT");

    private PortalAccess() {}

    /** Vrai si l'appelant est un parent restreint (rôle PARENT sans aucun rôle d'encadrement). */
    public static boolean isRestrictedParent(Collection<String> roles) {
        return roles != null && roles.contains("PARENT") && roles.stream().noneMatch(STAFF_ROLES::contains);
    }

    /**
     * Lève {@link AccessDeniedException} si un parent restreint tente d'accéder à un élève qui ne
     * lui est pas rattaché. Le personnel encadrant n'est jamais bloqué.
     *
     * @param isParentOf prédicat (userId, studentId) → vrai si le parent est rattaché à l'élève
     */
    public static void assertParentCanAccessStudent(Collection<String> roles, Long userId, Long studentId,
                                                    BiPredicate<Long, Long> isParentOf) {
        if (isRestrictedParent(roles) && !isParentOf.test(userId, studentId)) {
            throw new AccessDeniedException("Cet élève n'est pas rattaché à votre compte parent.");
        }
    }
}
