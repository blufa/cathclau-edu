package gn.lacaeducation.staff;

import java.math.BigDecimal;
import java.util.List;

/** API publique du module staff : personnel non-enseignant, pour la masse salariale (Vision Fondateur §10.2). */
public interface EmployeePublicInfoProvider {
    record EmployeeInfo(Long id, String fullName, String category, BigDecimal baseSalary, boolean active) {}

    /** Tout le personnel non-enseignant d'un tenant (actifs et inactifs — filtrer côté appelant). */
    List<EmployeeInfo> listEmployees(Long tenantId);
}
