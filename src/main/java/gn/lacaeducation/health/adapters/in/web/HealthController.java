package gn.lacaeducation.health.adapters.in.web;

import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.health.application.service.HealthService;
import gn.lacaeducation.health.application.service.HealthService.HealthRecordCommand;
import gn.lacaeducation.health.application.service.HealthService.VisitCommand;
import gn.lacaeducation.health.domain.HealthRecord;
import gn.lacaeducation.health.domain.InfirmaryVisit;
import gn.lacaeducation.health.domain.VisitOutcome;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Tag(name = "Health", description = "Fiche santé de l'élève (allergies, PAI, contacts d'urgence) et passages à l'infirmerie")
@RestController
@RequestMapping("/api/v1/health")
@RequiredArgsConstructor
public class HealthController {

    private final HealthService service;

    // ── Fiche santé ────────────────────────────────────────────────────────────

    public record HealthRecordRequest(String bloodGroup, String allergies, String chronicConditions,
                                      String medications, Boolean pai, String paiDetails, String dietaryRestrictions,
                                      String doctorName, String doctorPhone, String emergencyContactName,
                                      String emergencyContactPhone, String emergencyContactRelation,
                                      String insurance, String notes) {}

    @Operation(summary = "Récupérer la fiche santé d'un élève")
    @PreAuthorize("hasAuthority('sante:read')")
    @GetMapping("/records/student/{studentId}")
    public ResponseEntity<HealthRecord> getRecord(@PathVariable Long studentId) {
        return service.recordByStudent(studentId).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Créer / mettre à jour la fiche santé d'un élève")
    @PreAuthorize("hasAuthority('sante:manage')")
    @PutMapping("/records/student/{studentId}")
    public HealthRecord upsertRecord(@PathVariable Long studentId, @RequestBody HealthRecordRequest r) {
        return service.upsertRecord(new HealthRecordCommand(TenantContext.get(), studentId, r.bloodGroup(),
                r.allergies(), r.chronicConditions(), r.medications(), r.pai(), r.paiDetails(),
                r.dietaryRestrictions(), r.doctorName(), r.doctorPhone(), r.emergencyContactName(),
                r.emergencyContactPhone(), r.emergencyContactRelation(), r.insurance(), r.notes()));
    }

    // ── Passages à l'infirmerie ─────────────────────────────────────────────────

    public record VisitRequest(Long studentId, LocalDate visitDate, LocalTime visitTime, String reason,
                               String careGiven, BigDecimal temperature, VisitOutcome outcome) {}

    @Operation(summary = "Lister les passages à l'infirmerie (filtre élève)")
    @PreAuthorize("hasAuthority('sante:read')")
    @GetMapping("/visits")
    public List<InfirmaryVisit> listVisits(@RequestParam(required = false) Long studentId) {
        return service.visits(TenantContext.get(), studentId);
    }

    @Operation(summary = "Détail d'un passage")
    @GetMapping("/visits/{id}")
    public InfirmaryVisit findVisit(@PathVariable Long id) { return service.visit(id); }

    @Operation(summary = "Enregistrer un passage à l'infirmerie")
    @PreAuthorize("hasAuthority('sante:manage')")
    @PostMapping("/visits")
    public InfirmaryVisit recordVisit(@RequestBody VisitRequest r, @AuthenticationPrincipal Jwt jwt) {
        return service.recordVisit(new VisitCommand(TenantContext.get(), r.studentId(), r.visitDate(), r.visitTime(),
                r.reason(), r.careGiven(), r.temperature(), r.outcome(), currentUserId(jwt)));
    }

    @Operation(summary = "Modifier un passage")
    @PreAuthorize("hasAuthority('sante:manage')")
    @PutMapping("/visits/{id}")
    public InfirmaryVisit updateVisit(@PathVariable Long id, @RequestBody VisitRequest r) {
        return service.updateVisit(id, new VisitCommand(TenantContext.get(), r.studentId(), r.visitDate(),
                r.visitTime(), r.reason(), r.careGiven(), r.temperature(), r.outcome(), null));
    }

    @Operation(summary = "Marquer le parent comme prévenu")
    @PreAuthorize("hasAuthority('sante:manage')")
    @PatchMapping("/visits/{id}/notify-parent")
    public InfirmaryVisit notifyParent(@PathVariable Long id) { return service.markParentNotified(id); }

    @Operation(summary = "Supprimer un passage")
    @PreAuthorize("hasAuthority('sante:delete')")
    @DeleteMapping("/visits/{id}")
    public ResponseEntity<Void> deleteVisit(@PathVariable Long id) { service.deleteVisit(id); return ResponseEntity.noContent().build(); }

    private Long currentUserId(Jwt jwt) {
        try { return jwt == null ? null : Long.parseLong(jwt.getSubject()); } catch (RuntimeException e) { return null; }
    }
}
