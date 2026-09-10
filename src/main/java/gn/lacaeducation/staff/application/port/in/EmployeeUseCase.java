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
                                 String jobTitle, LocalDate hireDate, BigDecimal baseSalary, String cnssNumber) {}
    record UpdateEmployeeCommand(String employeeNumber, String firstName, String lastName, String phone,
                                 String email, EmployeeCategory category, String jobTitle, LocalDate hireDate,
                                 BigDecimal baseSalary, String cnssNumber,
                                 /** Fiche Teacher de la même personne si elle enseigne aussi (§payroll,
                                  * fiche de paie combinée) — null si elle n'enseigne pas. */
                                 Long linkedTeacherId) {}

    Employee create(CreateEmployeeCommand command);
    Employee update(Long id, UpdateEmployeeCommand command);
    Employee setActive(Long id, boolean active);
    Optional<Employee> findById(Long id);
    List<Employee> findByTenant(Long tenantId);
    List<Employee> findByCategory(Long tenantId, EmployeeCategory category);
    void delete(Long id);
}
