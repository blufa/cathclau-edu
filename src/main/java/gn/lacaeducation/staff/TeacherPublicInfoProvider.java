package gn.lacaeducation.staff;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/** API publique du module staff : informations d'un enseignant pour les documents (attestation, contrat…). */
public interface TeacherPublicInfoProvider {
    record TeacherInfo(Long id, String firstName, String lastName, String staffNumber,
                       String specialization, String email, String phone,
                       /** Photo (URL Cloudinary) pour les documents imprimés (carte, fiche de paie…) —
                        * null si jamais capturée. */
                       String photo,
                       /** Mode de rémunération saisi à la création de l'enseignant ("MENSUEL"/"HORAIRE"/
                        * "FORFAIT") et son taux — DISTINCT de {@code TeacherCompensation} (module payroll,
                        * créée seulement à la première fiche de paie générée). Permet à la masse salariale
                        * affichée au tableau de bord de compter un enseignant déjà connu même s'il n'a
                        * encore AUCUNE fiche de paie ni classe/matière assignée (§retour utilisateur : "même
                        * si il n'est pas affecté à une matière et une classe, le cumul de tout le salaire
                        * doit venir"). Null si jamais renseigné. */
                       String remunerationType, BigDecimal remunerationRate) {}

    Optional<TeacherInfo> findTeacher(Long teacherId);

    /** Tous les enseignants d'un tenant (reporting RH §10.3). */
    List<TeacherInfo> listTeachers(Long tenantId);

    /** Résout la fiche enseignant liée à un compte utilisateur (pour le cloisonnement par prof). */
    Optional<Long> findTeacherIdByUserId(Long userId);

    /** Compte utilisateur lié à une fiche enseignant (pour la paie : retrouver ses enfants scolarisés). */
    Optional<Long> findUserIdByTeacherId(Long teacherId);

    /** Niveaux (ids) où cet enseignant est déclaré habilité à enseigner — checklist "Niveaux enseignés"
     * de sa fiche (module staff), déclaration directe et volontaire, DISTINCTE des classes/matières
     * réellement assignées (§retour utilisateur : "base-toi sur le niveau enseigné, pas sur les
     * matières" — pour résoudre son cycle Maternelle/Primaire/Secondaire côté paie, voir
     * PayrollService#resolveCycleBucket). Vide si aucun niveau déclaré. */
    List<Long> findAssignedLevelIds(Long teacherId);
}
