package gn.lacaeducation.staff.adapters.out.persistence;

import gn.lacaeducation.staff.application.port.out.EmployeeRepository;
import gn.lacaeducation.staff.domain.Employee;
import gn.lacaeducation.staff.domain.EmployeeCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;

@Component @RequiredArgsConstructor
class EmployeePersistenceAdapter implements EmployeeRepository {

    private final EmployeeJpaRepository repo;

    @Override public Employee save(Employee e) { return toDomain(repo.save(toEntity(e))); }
    @Override public Optional<Employee> findById(Long id) { return repo.findById(id).map(this::toDomain); }
    @Override public List<Employee> findByTenantId(Long t) { return repo.findByTenantId(t).stream().map(this::toDomain).toList(); }
    @Override public List<Employee> findByTenantIdAndCategory(Long t, EmployeeCategory c) {
        return repo.findByTenantIdAndCategory(t, c).stream().map(this::toDomain).toList();
    }
    @Override public void deleteById(Long id) { repo.deleteById(id); }
    @Override public boolean existsById(Long id) { return repo.existsById(id); }
    @Override public boolean existsByTenantIdAndEmployeeNumber(Long t, String n) {
        return repo.existsByTenantIdAndEmployeeNumber(t, n);
    }

    private Employee toDomain(EmployeeJpaEntity e) {
        return Employee.builder()
                .id(e.getId()).tenantId(e.getTenantId()).userId(e.getUserId())
                .employeeNumber(e.getEmployeeNumber()).firstName(e.getFirstName()).lastName(e.getLastName())
                .phone(e.getPhone()).email(e.getEmail()).category(e.getCategory()).jobTitle(e.getJobTitle())
                .hireDate(e.getHireDate()).baseSalary(e.getBaseSalary()).cnssNumber(e.getCnssNumber())
                .active(e.getActive())
                .build();
    }

    private EmployeeJpaEntity toEntity(Employee d) {
        EmployeeJpaEntity e = d.getId() != null ? repo.findById(d.getId()).orElseGet(EmployeeJpaEntity::new)
                : new EmployeeJpaEntity();
        e.setTenantId(d.getTenantId());
        e.setUserId(d.getUserId());
        e.setEmployeeNumber(d.getEmployeeNumber());
        e.setFirstName(d.getFirstName());
        e.setLastName(d.getLastName());
        e.setPhone(d.getPhone());
        e.setEmail(d.getEmail());
        e.setCategory(d.getCategory());
        e.setJobTitle(d.getJobTitle());
        e.setHireDate(d.getHireDate());
        e.setBaseSalary(d.getBaseSalary());
        e.setCnssNumber(d.getCnssNumber());
        e.setActive(d.getActive() != null ? d.getActive() : Boolean.TRUE);
        return e;
    }
}
