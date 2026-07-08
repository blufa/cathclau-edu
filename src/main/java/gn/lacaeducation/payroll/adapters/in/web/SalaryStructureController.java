package gn.lacaeducation.payroll.adapters.in.web;

import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.payroll.application.service.PayrollService;
import gn.lacaeducation.payroll.domain.PayslipLineType;
import gn.lacaeducation.payroll.domain.RuleComputation;
import gn.lacaeducation.payroll.domain.SalaryRule;
import gn.lacaeducation.payroll.domain.SalaryStructure;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Tag(name = "Payroll — Salary Structures", description = "Structures salariales et règles de paie (configuration)")
@RestController
@RequestMapping("/api/v1/payroll/structures")
@PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','ACCOUNTANT')")
@RequiredArgsConstructor
public class SalaryStructureController {

    private final PayrollService service;

    public record StructureRequest(String name, String code, Boolean active) {}
    public record RuleRequest(String label, String code, String category, PayslipLineType lineType,
                              RuleComputation computation, BigDecimal value, Integer sequence) {}

    @Operation(summary = "Lister les structures salariales")
    @GetMapping
    public List<SalaryStructure> list() { return service.structures(TenantContext.get()); }

    @Operation(summary = "Créer une structure")
    @PostMapping
    public SalaryStructure create(@RequestBody StructureRequest r) {
        return service.createStructure(TenantContext.get(), r.name(), r.code());
    }

    @Operation(summary = "Modifier une structure")
    @PutMapping("/{id}")
    public SalaryStructure update(@PathVariable Long id, @RequestBody StructureRequest r) {
        return service.updateStructure(id, r.name(), r.code(), r.active() == null || r.active());
    }

    @Operation(summary = "Supprimer une structure")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) { service.deleteStructure(id); return ResponseEntity.noContent().build(); }

    @Operation(summary = "Lister les règles d'une structure")
    @GetMapping("/{id}/rules")
    public List<SalaryRule> rules(@PathVariable Long id) { return service.rules(id); }

    @Operation(summary = "Ajouter une règle à une structure")
    @PostMapping("/{id}/rules")
    public SalaryRule addRule(@PathVariable Long id, @RequestBody RuleRequest r) {
        return service.addRule(TenantContext.get(), id, SalaryRule.builder()
                .label(r.label()).code(r.code()).category(r.category())
                .lineType(r.lineType()).computation(r.computation()).value(r.value()).sequence(r.sequence()).build());
    }

    @Operation(summary = "Supprimer une règle")
    @DeleteMapping("/rules/{ruleId}")
    public ResponseEntity<Void> deleteRule(@PathVariable Long ruleId) { service.deleteRule(ruleId); return ResponseEntity.noContent().build(); }
}
