package gn.lacaeducation.billing.adapters.in.web;
import gn.lacaeducation.billing.application.port.in.SchoolFeeUseCase;
import gn.lacaeducation.billing.domain.FeeType;
import gn.lacaeducation.billing.domain.SchoolFee;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;
@Tag(name = "School Fees", description = "Configure school fee schedules by class and fee type")
@RestController @RequestMapping("/api/v1/billing/fees") @RequiredArgsConstructor
public class SchoolFeeController {
    private final SchoolFeeUseCase useCase;
    public record CreateRequest(Long tenantId, Long classId, FeeType type, BigDecimal amount, String academicYear, String label) {}
    public record UpdateRequest(Long classId, FeeType type, BigDecimal amount, String academicYear, String label) {}
    @Operation(summary = "Create a new school fee configuration")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','ACCOUNTANT')")
    @PostMapping public ResponseEntity<SchoolFee> create(@RequestBody CreateRequest r) {
        return ResponseEntity.ok(useCase.create(new SchoolFeeUseCase.CreateFeeCommand(r.tenantId(), r.classId(), r.type(), r.amount(), r.academicYear(), r.label())));
    }
    @Operation(summary = "Update an existing school fee configuration")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','ACCOUNTANT')")
    @PutMapping("/{id}") public ResponseEntity<SchoolFee> update(@PathVariable Long id, @RequestBody UpdateRequest r) {
        return ResponseEntity.ok(useCase.update(id, new SchoolFeeUseCase.UpdateFeeCommand(r.classId(), r.type(), r.amount(), r.academicYear(), r.label())));
    }
    @Operation(summary = "Get a school fee configuration by ID")
    @GetMapping("/{id}") public ResponseEntity<SchoolFee> findById(@PathVariable Long id) {
        return useCase.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
    @Operation(summary = "List all school fee configurations for a tenant")
    @GetMapping("/tenant/{tenantId}") public List<SchoolFee> findByTenant(@PathVariable Long tenantId) { return useCase.findByTenantId(tenantId); }
    @Operation(summary = "List all school fee configurations for a class")
    @GetMapping("/class/{classId}") public List<SchoolFee> findByClass(@PathVariable Long classId) { return useCase.findByClassId(classId); }
    @Operation(summary = "Delete a school fee configuration")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','ACCOUNTANT')")
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable Long id) { useCase.delete(id); return ResponseEntity.noContent().build(); }
}
