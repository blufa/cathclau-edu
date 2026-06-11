package gn.lacaeducation.billing.adapters.in.web;
import gn.lacaeducation.billing.application.port.in.SchoolFeeUseCase;
import gn.lacaeducation.billing.domain.FeeType;
import gn.lacaeducation.billing.domain.SchoolFee;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;
@RestController @RequestMapping("/api/billing/fees") @RequiredArgsConstructor
public class SchoolFeeController {
    private final SchoolFeeUseCase useCase;
    public record CreateRequest(Long tenantId, Long classId, FeeType type, BigDecimal amount, String academicYear) {}
    public record UpdateRequest(Long classId, FeeType type, BigDecimal amount, String academicYear) {}
    @PostMapping public ResponseEntity<SchoolFee> create(@RequestBody CreateRequest r) {
        return ResponseEntity.ok(useCase.create(new SchoolFeeUseCase.CreateFeeCommand(r.tenantId(), r.classId(), r.type(), r.amount(), r.academicYear())));
    }
    @PutMapping("/{id}") public ResponseEntity<SchoolFee> update(@PathVariable Long id, @RequestBody UpdateRequest r) {
        return ResponseEntity.ok(useCase.update(id, new SchoolFeeUseCase.UpdateFeeCommand(r.classId(), r.type(), r.amount(), r.academicYear())));
    }
    @GetMapping("/{id}") public ResponseEntity<SchoolFee> findById(@PathVariable Long id) {
        return useCase.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
    @GetMapping("/tenant/{tenantId}") public List<SchoolFee> findByTenant(@PathVariable Long tenantId) { return useCase.findByTenantId(tenantId); }
    @GetMapping("/class/{classId}") public List<SchoolFee> findByClass(@PathVariable Long classId) { return useCase.findByClassId(classId); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable Long id) { useCase.delete(id); return ResponseEntity.noContent().build(); }
}
