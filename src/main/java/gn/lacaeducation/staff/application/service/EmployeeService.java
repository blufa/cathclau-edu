package gn.lacaeducation.staff.application.service;

import gn.lacaeducation.common.ConflictException;
import gn.lacaeducation.common.MatriculeAllocator;
import gn.lacaeducation.common.PersonName;
import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.common.TenantInfoProvider;
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
    private final TenantInfoProvider tenantInfoProvider;

    @Override
    public Employee create(CreateEmployeeCommand c) {
        String matricule = resolveEmployeeNumber(c.tenantId(), c.employeeNumber());
        return repo.save(Employee.builder()
                .tenantId(c.tenantId()).userId(c.userId()).employeeNumber(matricule)
                .firstName(PersonName.capitalizeFirstName(c.firstName()))
                .lastName(PersonName.upperCaseLastName(c.lastName())).phone(c.phone()).email(c.email())
                .category(c.category()).jobTitle(c.jobTitle()).hireDate(c.hireDate())
                .baseSalary(c.baseSalary()).cnssNumber(c.cnssNumber()).bankAccount(c.bankAccount()).active(true)
                .build());
    }

    /** Même logique que {@code EnseignantService#resolveStaffNumber} : valide un matricule fourni, ou
     * en génère un auto — le personnel non-enseignant n'en recevait jamais faute d'auto-génération ici. */
    private String resolveEmployeeNumber(Long tenantId, String provided) {
        if (provided != null && !provided.isBlank()) {
            if (repo.existsByTenantIdAndEmployeeNumber(tenantId, provided)) {
                throw new ConflictException("Un employé porte déjà le matricule " + provided);
            }
            return provided;
        }
        long seq = repo.findByTenantId(tenantId).size() + 1L;
        String candidate;
        do {
            candidate = MatriculeAllocator.candidate(tenantInfoProvider, tenantId, seq++);
        } while (repo.existsByTenantIdAndEmployeeNumber(tenantId, candidate));
        return candidate;
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
        // Ne lie un compte que s'il est fourni — ne jamais délier un compte déjà existant via une
        // simple mise à jour de profil (voir javadoc de UpdateEmployeeCommand#userId).
        if (c.userId() != null) e.setUserId(c.userId());
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
