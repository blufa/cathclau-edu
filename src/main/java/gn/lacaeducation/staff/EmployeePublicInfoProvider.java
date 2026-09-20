package gn.lacaeducation.staff;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/** API publique du module staff : personnel non-enseignant, pour la masse salariale (Vision Fondateur §10.2)
 * et la présence (§attendance). */
public interface EmployeePublicInfoProvider {
    record EmployeeInfo(Long id, String fullName, String category, BigDecimal baseSalary, boolean active,
                        String phone, String email) {}

    /** Tout le personnel non-enseignant d'un tenant (actifs et inactifs — filtrer côté appelant). */
    List<EmployeeInfo> listEmployees(Long tenantId);

    /** Une fiche personnel par id (pour le pointage de présence : nom affiché, existence vérifiée). */
    Optional<EmployeeInfo> findEmployee(Long id);

    /** Résout la fiche personnel liée à un compte utilisateur (pour reconnaître un enfant de membre du
     * personnel non-enseignant — §billing, avantage personnel — au même titre qu'un enfant d'enseignant). */
    Optional<Long> findEmployeeIdByUserId(Long userId);
}
