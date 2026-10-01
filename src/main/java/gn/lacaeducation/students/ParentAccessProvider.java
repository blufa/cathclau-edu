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

    /** Comme {@link #findChildStudentIds}, mais restreint aux élèves ayant une inscription VALIDÉE —
     * pour les usages qui ne doivent compter que les enfants effectivement scolarisés (ex. retenue sur
     * salaire des frais de scolarité, §7 payroll), contrairement à l'accès au portail parent qui reste
     * volontairement permissif (historique) sur {@link #findChildStudentIds}. */
    java.util.List<Long> findChildStudentIdsWithValidatedEnrollment(Long userId);

    /** Identifiants des comptes utilisateurs des parents d'un élève (pour notifier ; vide si aucun). */
    java.util.List<Long> findParentUserIdsByStudent(Long studentId);

    /** Identifiant du compte utilisateur lié à ce parent (vide si le parent n'a pas de compte, ou
     * introuvable) — pour vérifier si ce parent correspond aussi à un membre du personnel (même
     * {@code userId} partagé que {@code Teacher}/{@code Employee}, voir refonte inscription §25). */
    java.util.Optional<Long> findUserIdByParentId(Long parentId);

    /** Lien direct vers une fiche Teacher/Employee, SANS compte de connexion — pour le personnel qui ne
     *  veut/ne peut pas en avoir un (ex. pas de smartphone Android). {@code staffType} vaut "TEACHER"
     *  ou "EMPLOYEE". Alternative à {@link #findUserIdByParentId} pour la détection "enfant du
     *  personnel" (voir {@code StaffChildBenefitService#isStaffChild}). */
    record DirectStaffLink(String staffType, Long staffId) {}

    /** Liens directs personnel (sans compte) de TOUTES les fiches Parent de cet élève — vide si aucune. */
    java.util.List<DirectStaffLink> findDirectStaffLinksByStudent(Long studentId);

    /** Lien direct personnel (sans compte) de CETTE fiche Parent précise — vide si aucun. */
    java.util.Optional<DirectStaffLink> findDirectStaffLinkByParentId(Long parentId);
}
