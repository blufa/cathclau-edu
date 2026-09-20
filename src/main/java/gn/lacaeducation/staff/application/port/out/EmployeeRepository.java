package gn.lacaeducation.staff.application.port.out;

import gn.lacaeducation.staff.domain.Employee;
import gn.lacaeducation.staff.domain.EmployeeCategory;
import java.util.List;
import java.util.Optional;

public interface EmployeeRepository {
    Employee save(Employee employee);
    Optional<Employee> findById(Long id);
    List<Employee> findByTenantId(Long tenantId);
    List<Employee> findByTenantIdAndCategory(Long tenantId, EmployeeCategory category);
    void deleteById(Long id);
    boolean existsById(Long id);
    boolean existsByTenantIdAndEmployeeNumber(Long tenantId, String employeeNumber);
    Optional<Employee> findByUserId(Long userId);
}
