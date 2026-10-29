package gn.lacaeducation.payroll.adapters.in.web;

import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.payroll.application.port.in.SalaryFeeDeductionUseCase;
import gn.lacaeducation.payroll.application.port.in.SalaryFeeDeductionUseCase.FeeDeduction;
import gn.lacaeducation.payroll.application.port.in.SalaryFeeDeductionUseCase.RuleCommand;
import gn.lacaeducation.payroll.domain.EmployeeType;
import gn.lacaeducation.payroll.domain.SalaryFeeDeductionRule;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/**
 * Règles paramétrables de retenue sur salaire des frais scolaires (§7) et calcul de la retenue d'un
 * employé. Configuration réservée à la direction / comptabilité.
 */
@Tag(name = "Salary Fee Deductions", description = "Retenue sur salaire des frais scolaires du personnel (§7)")
@RestController
@RequestMapping("/api/v1/payroll/fee-deduction-rules")
@RequiredArgsConstructor
public class SalaryFeeDeductionController {

    private final SalaryFeeDeductionUseCase useCase;

    @Operation(summary = "Créer une règle de retenue sur salaire des frais scolaires")
    @PreAuthorize("hasAuthority('paie:manage')")
    @PostMapping
    public ResponseEntity<SalaryFeeDeductionRule> create(@RequestBody RuleCommand cmd) {
        SalaryFeeDeductionRule r = useCase.create(TenantContext.get(), cmd);
        return ResponseEntity.created(URI.create("/api/v1/payroll/fee-deduction-rules/" + r.getId())).body(r);
    }

    @Operation(summary = "Lister les règles du tenant")
    @PreAuthorize("hasAuthority('paie:manage')")
    @GetMapping
    public List<SalaryFeeDeductionRule> list() {
        return useCase.findByTenant(TenantContext.get());
    }

    @Operation(summary = "Modifier une règle")
    @PreAuthorize("hasAuthority('paie:manage')")
    @PutMapping("/{id}")
    public SalaryFeeDeductionRule update(@PathVariable Long id, @RequestBody RuleCommand cmd) {
        return useCase.update(id, cmd);
    }

    @Operation(summary = "Supprimer une règle")
    @PreAuthorize("hasAuthority('paie:manage')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        useCase.delete(id);
    }

    @Operation(summary = "Retenue de frais scolaires calculée pour un employé (aperçu avant paie)")
    @PreAuthorize("hasAuthority('paie:manage')")
    @GetMapping("/preview")
    public FeeDeduction preview(@RequestParam Long teacherId,
                                 @RequestParam(required = false, defaultValue = "TEACHER") EmployeeType employeeType) {
        return useCase.computeFeeDeduction(TenantContext.get(), employeeType, teacherId);
    }

    @Operation(summary = "Détail de la retenue \"avantage personnel\", enfant par enfant : factures "
            + "retenues et frais facultatifs pas encore facturés (modal de détail de la fiche de paie)")
    @PreAuthorize("hasAuthority('paie:manage')")
    @GetMapping("/detail")
    public List<SalaryFeeDeductionUseCase.ChildDeductionDetail> detail(
            @RequestParam Long staffId,
            @RequestParam(required = false, defaultValue = "TEACHER") EmployeeType employeeType,
            @RequestParam int year, @RequestParam int month) {
        // periodLabel laissé null : dérivé depuis periodStart côté billing (même règle que la retenue
        // réelle à la mise en paiement, voir InvoiceService#resolveTargetPeriodLabel) — jamais recalculé
        // ici (hors frontière de module, gn.lacaeducation.billing.domain n'est pas une API publique).
        java.time.LocalDate periodStart = java.time.LocalDate.of(year, month, 1);
        return useCase.staffBenefitDetail(TenantContext.get(), employeeType, staffId, periodStart, null);
    }

    @Operation(summary = "Exclure (ou réintégrer) spécifiquement UN enfant de la retenue sur salaire de "
            + "ce salarié — par enfant, pour le cas où certains enfants du même salarié bénéficient de "
            + "l'avantage personnel et d'autres sont payés directement par un parent")
    @PreAuthorize("hasAuthority('paie:manage')")
    @PatchMapping("/child-exclusion")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setChildExcluded(@RequestParam Long staffId,
                                  @RequestParam(required = false, defaultValue = "TEACHER") EmployeeType employeeType,
                                  @RequestParam Long studentId, @RequestParam boolean excluded) {
        useCase.setChildStaffDeductionExcluded(TenantContext.get(), employeeType, staffId, studentId, excluded);
    }
}
