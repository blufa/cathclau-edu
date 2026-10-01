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
@Tag(name = "School Fees", description = "Configurer les frais scolaires par classe et type de frais")
@RestController @RequestMapping("/api/v1/billing/fees") @RequiredArgsConstructor
public class SchoolFeeController {
    private final SchoolFeeUseCase useCase;
    public record CreateRequest(Long tenantId, Long classId, Long routeId, FeeType type, BigDecimal amount, String academicYear, String label) {}
    public record UpdateRequest(Long classId, Long routeId, FeeType type, BigDecimal amount, String academicYear, String label) {}
    @Operation(summary = "Créer une nouvelle configuration de frais scolaire")
    @PreAuthorize("hasAuthority('frais_scolaire:create')")
    @PostMapping public ResponseEntity<SchoolFee> create(@RequestBody CreateRequest r) {
        return ResponseEntity.ok(useCase.create(new SchoolFeeUseCase.CreateFeeCommand(r.tenantId(), r.classId(), r.routeId(), r.type(), r.amount(), r.academicYear(), r.label())));
    }
    @Operation(summary = "Modifier une configuration de frais scolaire existante")
    @PreAuthorize("hasAuthority('frais_scolaire:update')")
    @PutMapping("/{id}") public ResponseEntity<SchoolFee> update(@PathVariable Long id, @RequestBody UpdateRequest r) {
        return ResponseEntity.ok(useCase.update(id, new SchoolFeeUseCase.UpdateFeeCommand(r.classId(), r.routeId(), r.type(), r.amount(), r.academicYear(), r.label())));
    }
    @Operation(summary = "Consulter une configuration de frais scolaire par son ID")
    @PreAuthorize("hasAuthority('frais_scolaire:read')")
    @GetMapping("/{id}") public ResponseEntity<SchoolFee> findById(@PathVariable Long id) {
        return useCase.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
    @Operation(summary = "Lister les configurations de frais scolaires d'un établissement")
    @PreAuthorize("hasAuthority('frais_scolaire:read')")
    @GetMapping("/tenant/{tenantId}") public List<SchoolFee> findByTenant(@PathVariable Long tenantId) { return useCase.findByTenantId(tenantId); }
    @Operation(summary = "Lister les configurations de frais scolaires d'une classe")
    @PreAuthorize("hasAuthority('frais_scolaire:read')")
    @GetMapping("/class/{classId}") public List<SchoolFee> findByClass(@PathVariable Long classId) { return useCase.findByClassId(classId); }
    @Operation(summary = "Lister les configurations de frais de TRANSPORT d'un itinéraire")
    @PreAuthorize("hasAuthority('frais_scolaire:read')")
    @GetMapping("/route/{routeId}") public List<SchoolFee> findByRoute(@PathVariable Long routeId) { return useCase.findByRouteId(routeId); }
    @Operation(summary = "Supprimer une configuration de frais scolaire")
    @PreAuthorize("hasAuthority('frais_scolaire:delete')")
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable Long id) { useCase.delete(id); return ResponseEntity.noContent().build(); }
}
