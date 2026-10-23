package gn.lacaeducation.staff;

import java.util.List;
import java.util.Optional;

/** API publique du module staff : informations d'un enseignant pour les documents (attestation, contrat…). */
public interface TeacherPublicInfoProvider {
    record TeacherInfo(Long id, String firstName, String lastName, String staffNumber,
                       String specialization, String email, String phone,
                       /** Photo (URL Cloudinary) pour les documents imprimés (carte, fiche de paie…) —
                        * null si jamais capturée. */
                       String photo) {}

    Optional<TeacherInfo> findTeacher(Long teacherId);

    /** Tous les enseignants d'un tenant (reporting RH §10.3). */
    List<TeacherInfo> listTeachers(Long tenantId);

    /** Résout la fiche enseignant liée à un compte utilisateur (pour le cloisonnement par prof). */
    Optional<Long> findTeacherIdByUserId(Long userId);

    /** Compte utilisateur lié à une fiche enseignant (pour la paie : retrouver ses enfants scolarisés). */
    Optional<Long> findUserIdByTeacherId(Long teacherId);
}
