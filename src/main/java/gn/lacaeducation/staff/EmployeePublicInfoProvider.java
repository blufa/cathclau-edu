package gn.lacaeducation.staff;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/** API publique du module staff : personnel non-enseignant, pour la masse salariale (Vision Fondateur §10.2)
 * et la présence (§attendance). */
public interface EmployeePublicInfoProvider {
    record EmployeeInfo(Long id, String fullName, String category, BigDecimal baseSalary, boolean active,
                        String phone, String email,
                        /** Intitulé de poste libre (ex. « Principal », « Comptable ») — pour les
                         * documents imprimés (fiche de paie…), distinct de {@link #category}. */
                        String jobTitle,
                        /** Photo (URL Cloudinary) pour les documents imprimés — null si jamais capturée. */
                        String photo) {}

    /** Tout le personnel non-enseignant d'un tenant (actifs et inactifs — filtrer côté appelant). */
    List<EmployeeInfo> listEmployees(Long tenantId);

    /** Une fiche personnel par id (pour le pointage de présence : nom affiché, existence vérifiée). */
    Optional<EmployeeInfo> findEmployee(Long id);

    /** Résout la fiche personnel liée à un compte utilisateur (pour reconnaître un enfant de membre du
     * personnel non-enseignant — §billing, avantage personnel — au même titre qu'un enfant d'enseignant). */
    Optional<Long> findEmployeeIdByUserId(Long userId);

    /** Compte utilisateur lié à une fiche personnel non-enseignant (pour la paie : retrouver ses enfants
     * scolarisés — symétrique de {@code TeacherPublicInfoProvider#findUserIdByTeacherId} pour les
     * salariés administratifs/direction/support). */
    Optional<Long> findUserIdByEmployeeId(Long employeeId);
}
