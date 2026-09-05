package gn.lacaeducation.students;
import java.util.List;
import java.util.Optional;
public interface StudentDataProvider {
    Optional<StudentPublicInfo> findStudent(Long studentId);
    List<ParentPublicInfo> findParentsByStudent(Long studentId);
    /** Students with a non-cancelled enrollment in the class, with their contact (phone/email). */
    List<StudentPublicInfo> findStudentsByClass(Long classId);
    /** La classe de l'inscription validée la plus récente de l'élève (sa classe courante), si elle existe. */
    Optional<Long> findCurrentClassId(Long studentId);
    /** Identifiant élève lié au compte utilisateur (portail élève), si {@code userId} en a un. */
    Optional<Long> findStudentIdByUserId(Long userId);
    /** Tous les parents du tenant (diffusion de masse « tous les parents ») — y compris ceux dont
     * aucun enfant n'a l'accès plateforme, pour les écrans d'administration (ne pas utiliser pour un
     * envoi de notification, voir {@link #findAllParentsWithPlatformAccess(Long)}). */
    List<ParentPublicInfo> findAllParents(Long tenantId);
    /** Le parent a-t-il accepté l'accès à la plateforme pour l'inscription validée la plus récente de
     * cet élève (frais annexe optionnel, §billing) ? False si aucune inscription validée. Conditionne
     * l'envoi des notifications SMS/email (paiement, absence, retard, annonce...) à ce parent —
     * §communication : pas de notification sans accès plateforme accepté. */
    boolean hasPlatformAccessOptIn(Long studentId);
    /** Comme {@link #findAllParents(Long)}, mais restreint aux parents ayant au moins un enfant avec
     * l'accès plateforme accepté — pour la diffusion de masse « tous les parents » (§communication). */
    List<ParentPublicInfo> findAllParentsWithPlatformAccess(Long tenantId);
}
