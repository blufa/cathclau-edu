package gn.lacaeducation.staff;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/** API publique du module staff : personnel non-enseignant, pour la masse salariale (Vision Fondateur §10.2)
 * et la présence (§attendance). */
public interface EmployeePublicInfoProvider {
    record EmployeeInfo(Long id, String fullName, String category, BigDecimal baseSalary, boolean active) {}

    /** Tout le personnel non-enseignant d'un tenant (actifs et inactifs — filtrer côté appelant). */
    List<EmployeeInfo> listEmployees(Long tenantId);

    /** Une fiche personnel par id (pour le pointage de présence : nom affiché, existence vérifiée). */
    Optional<EmployeeInfo> findEmployee(Long id);
}
