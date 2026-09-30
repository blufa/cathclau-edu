package gn.lacaeducation.staff.application.port.in;

import gn.lacaeducation.staff.domain.Employee;
import gn.lacaeducation.staff.domain.EmployeeCategory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EmployeeUseCase {
    record CreateEmployeeCommand(Long tenantId, Long userId, String employeeNumber, String firstName,
                                 String lastName, String phone, String email, EmployeeCategory category,
                                 String jobTitle, LocalDate hireDate, BigDecimal baseSalary, String cnssNumber,
                                 /** Compte bancaire (RIB/IBAN) optionnel, pour le fichier bancaire de paie. */
                                 String bankAccount) {}
    record UpdateEmployeeCommand(String employeeNumber, String firstName, String lastName, String phone,
                                 String email, EmployeeCategory category, String jobTitle, LocalDate hireDate,
                                 BigDecimal baseSalary, String cnssNumber, String bankAccount,
                                 /** Fiche Teacher de la même personne si elle enseigne aussi (§payroll,
                                  * fiche de paie combinée) — null si elle n'enseigne pas. */
                                 Long linkedTeacherId,
                                 /** Lie un compte de connexion créé APRÈS coup (ex. « Créer un accès » oublié à la
                                  *  création) — null = ne touche pas au lien existant, jamais utilisé pour le
                                  *  retirer. Voir StaffSearchController, qui exige ce lien pour proposer ce
                                  *  personnel au rattachement parent sur la fiche d'inscription. */
                                 Long userId) {}

    Employee create(CreateEmployeeCommand command);
    Employee update(Long id, UpdateEmployeeCommand command);
    Employee updatePhoto(Long id, String photoUrl);
    Employee setActive(Long id, boolean active);
    Optional<Employee> findById(Long id);
    List<Employee> findByTenant(Long tenantId);
    List<Employee> findByCategory(Long tenantId, EmployeeCategory category);
    void delete(Long id);
}
