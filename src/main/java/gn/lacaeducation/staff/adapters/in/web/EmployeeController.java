package gn.lacaeducation.staff.adapters.in.web;

import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.staff.application.port.in.EmployeeUseCase;
import gn.lacaeducation.staff.application.port.in.EmployeeUseCase.CreateEmployeeCommand;
import gn.lacaeducation.staff.application.port.in.EmployeeUseCase.UpdateEmployeeCommand;
import gn.lacaeducation.staff.domain.Employee;
import gn.lacaeducation.staff.domain.EmployeeCategory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;

@Tag(name = "Employees", description = "Personnel non-enseignant (administratif, direction, soutien) — §7.1")
@RestController @RequestMapping("/api/v1/staff/employees") @RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeUseCase useCase;

    @Operation(summary = "Créer un employé non-enseignant")
    @PreAuthorize("hasAuthority('personnel:manage')")
    @PostMapping
    public ResponseEntity<Employee> create(@RequestBody CreateEmployeeCommand cmd) {
        Employee e = useCase.create(new CreateEmployeeCommand(TenantContext.get(), cmd.userId(),
                cmd.employeeNumber(), cmd.firstName(), cmd.lastName(), cmd.phone(), cmd.email(),
                cmd.category(), cmd.jobTitle(), cmd.hireDate(), cmd.baseSalary(), cmd.cnssNumber(),
                cmd.bankAccount()));
        return ResponseEntity.created(URI.create("/api/v1/staff/employees/" + e.getId())).body(e);
    }

    @Operation(summary = "Lister le personnel non-enseignant (filtre catégorie optionnel)")
    @PreAuthorize("hasAuthority('personnel:read')")
    @GetMapping
    public List<Employee> list(@RequestParam(required = false) EmployeeCategory category) {
        return category != null
                ? useCase.findByCategory(TenantContext.get(), category)
                : useCase.findByTenant(TenantContext.get());
    }

    @Operation(summary = "Détail d'un employé")
    @PreAuthorize("hasAuthority('personnel:read')")
    @GetMapping("/{id}")
    public Employee findById(@PathVariable Long id) {
        return useCase.findById(id).orElseThrow(() -> new ResourceNotFoundException("Employee", id));
    }

    @Operation(summary = "Modifier un employé")
    @PreAuthorize("hasAuthority('personnel:manage')")
    @PutMapping("/{id}")
    public Employee update(@PathVariable Long id, @RequestBody UpdateEmployeeCommand cmd) {
        return useCase.update(id, cmd);
    }

    @Operation(summary = "Définir la photo d'un employé (badge personnel)")
    @PreAuthorize("hasAuthority('personnel:manage')")
    @PatchMapping("/{id}/photo")
    public Employee updatePhoto(@PathVariable Long id, @RequestParam String photoUrl) {
        return useCase.updatePhoto(id, photoUrl);
    }

    @Operation(summary = "Activer un employé")
    @PreAuthorize("hasAuthority('personnel:manage')")
    @PatchMapping("/{id}/activate")
    public Employee activate(@PathVariable Long id) { return useCase.setActive(id, true); }

    @Operation(summary = "Désactiver un employé")
    @PreAuthorize("hasAuthority('personnel:manage')")
    @PatchMapping("/{id}/deactivate")
    public Employee deactivate(@PathVariable Long id) { return useCase.setActive(id, false); }

    @Operation(summary = "Supprimer un employé")
    @PreAuthorize("hasAuthority('personnel:manage')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        useCase.delete(id);
        return ResponseEntity.noContent().build();
    }
}
