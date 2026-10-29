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

    /** Comme {@link #findChildStudentIds}, mais restreint aux élèves ayant une inscription ACTIVE (toute
     * inscription hors CANCELLED/ABANDONED, y compris PENDING jamais validée) — pour les usages qui ne
     * doivent compter que les enfants effectivement scolarisés (ex. retenue sur salaire des frais de
     * scolarité, §7 payroll), contrairement à l'accès au portail parent qui reste volontairement
     * permissif (historique) sur {@link #findChildStudentIds}. PENDING compte volontairement (retour
     * comptable : un membre du personnel ne doit pas pouvoir échapper à la retenue en laissant
     * l'inscription de son enfant sans validation). */
    java.util.List<Long> findChildStudentIdsEnrolled(Long userId);

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

    /** Comme {@link #findChildStudentIdsEnrolled}, mais pour un membre du personnel SANS compte de
     * connexion (lien direct {@code staffType}/{@code staffId}, voir {@link DirectStaffLink}). Sans ce
     * chemin, la retenue sur salaire (§7 payroll) ne trouvait jamais les enfants d'un personnel qui n'a
     * pas de compte — seul {@link #findChildStudentIdsEnrolled} (compte partagé) était consulté côté
     * paie, alors que l'inscription "Personnel de l'école" accepte les deux. */
    java.util.List<Long> findChildStudentIdsEnrolledByStaffLink(String staffType, Long staffId);

    /** Vrai si cet élève est exclu de la retenue salariale automatique — soit parce qu'AU MOINS UN
     * parent lié a explicitement choisi de prendre en charge lui-même les frais de TOUS ses enfants
     * (champ {@code excludeFromAutoDeduction} sur la fiche Parent, réglage global), SOIT parce que ce
     * lien élève-parent précis a été exclu individuellement (champ {@code excludedFromStaffDeduction}
     * sur le lien {@code StudentParent} — voir {@link #setChildStaffDeductionExcluded}, réglage par
     * enfant, pour le cas où certains enfants du même salarié bénéficient de l'avantage et d'autres non,
     * §retour comptable). Dans les deux cas, AUCUNE retenue automatique, ni règle à montant fixe
     * ({@code SalaryFeeDeductionService#computeFeeDeduction}) ni solde réel
     * ({@code computeStaffBenefitRemainder}), ni tag "avantage personnel" sur leurs factures
     * ({@code StaffChildBenefitService#isStaffChild}). */
    boolean hasOptedOutOfStaffAutoDeduction(Long studentId);

    /** Exclut (ou réintègre) spécifiquement CET enfant de la retenue salariale automatique de ce
     * salarié — par ENFANT, contrairement au réglage global {@code Parent#excludeFromAutoDeduction} qui
     * s'applique à tous les enfants de ce parent (§retour comptable : "il y a des enfants qui sont [à
     * charge de l'avantage] et d'autres [payés directement par le parent]"). Résout le lien staff↔enfant
     * par compte ({@code userId}, peut être {@code null}) ET par lien direct personnel
     * ({@code staffType}/{@code staffId}, voir {@link DirectStaffLink}), comme
     * {@link #findChildStudentIdsEnrolled}/{@link #findChildStudentIdsEnrolledByStaffLink} — sans effet
     * si aucun lien ne rattache ce studentId à ce salarié. */
    void setChildStaffDeductionExcluded(Long userId, String staffType, Long staffId, Long studentId, boolean excluded);
}
