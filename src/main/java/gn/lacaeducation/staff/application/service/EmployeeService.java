package gn.lacaeducation.staff.application.service;

import gn.lacaeducation.common.ConflictException;
import gn.lacaeducation.common.PersonName;
import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.staff.application.port.in.EmployeeUseCase;
import gn.lacaeducation.staff.application.port.out.EmployeeRepository;
import gn.lacaeducation.staff.domain.Employee;
import gn.lacaeducation.staff.domain.EmployeeCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/** Gestion du personnel non-enseignant (administratif, direction, soutien) — §7.1. */
@Service @Transactional @RequiredArgsConstructor
public class EmployeeService implements EmployeeUseCase {

    private final EmployeeRepository repo;

    @Override
    public Employee create(CreateEmployeeCommand c) {
        if (c.employeeNumber() != null && !c.employeeNumber().isBlank()
                && repo.existsByTenantIdAndEmployeeNumber(c.tenantId(), c.employeeNumber())) {
            throw new ConflictException("Un employé porte déjà le matricule " + c.employeeNumber());
        }
        return repo.save(Employee.builder()
                .tenantId(c.tenantId()).userId(c.userId()).employeeNumber(c.employeeNumber())
                .firstName(PersonName.capitalizeFirstName(c.firstName()))
                .lastName(PersonName.upperCaseLastName(c.lastName())).phone(c.phone()).email(c.email())
                .category(c.category()).jobTitle(c.jobTitle()).hireDate(c.hireDate())
                .baseSalary(c.baseSalary()).cnssNumber(c.cnssNumber()).bankAccount(c.bankAccount()).active(true)
                .build());
    }

    @Override
    public Employee update(Long id, UpdateEmployeeCommand c) {
        Employee e = load(id);
        e.setEmployeeNumber(c.employeeNumber());
        e.setFirstName(PersonName.capitalizeFirstName(c.firstName()));
        e.setLastName(PersonName.upperCaseLastName(c.lastName()));
        e.setPhone(c.phone());
        e.setEmail(c.email());
        e.setCategory(c.category());
        e.setJobTitle(c.jobTitle());
        e.setHireDate(c.hireDate());
        e.setBaseSalary(c.baseSalary());
        e.setCnssNumber(c.cnssNumber());
        e.setBankAccount(c.bankAccount());
        e.setLinkedTeacherId(c.linkedTeacherId());
        return repo.save(e);
    }

    @Override
    public Employee updatePhoto(Long id, String photoUrl) {
        Employee e = load(id);
        e.setPhoto(photoUrl);
        return repo.save(e);
    }

    @Override
    public Employee setActive(Long id, boolean active) {
        Employee e = load(id);
        e.setActive(active);
        return repo.save(e);
    }

    @Override @Transactional(readOnly = true)
    public Optional<Employee> findById(Long id) { return repo.findById(id); }

    @Override @Transactional(readOnly = true)
    public List<Employee> findByTenant(Long tenantId) { return repo.findByTenantId(tenantId); }

    @Override @Transactional(readOnly = true)
    public List<Employee> findByCategory(Long tenantId, EmployeeCategory category) {
        return repo.findByTenantIdAndCategory(tenantId, category);
    }

    @Override
    public void delete(Long id) { repo.deleteById(id); }

    private Employee load(Long id) {
        return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Employee", id));
    }
}
