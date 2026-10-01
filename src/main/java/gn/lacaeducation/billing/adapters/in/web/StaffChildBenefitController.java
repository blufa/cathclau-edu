package gn.lacaeducation.billing.adapters.in.web;

import gn.lacaeducation.billing.application.port.in.StaffChildBenefitUseCase;
import gn.lacaeducation.billing.application.port.in.StaffChildBenefitUseCase.PolicyCommand;
import gn.lacaeducation.billing.application.port.in.StaffChildBenefitUseCase.StaffCoverage;
import gn.lacaeducation.billing.application.port.in.StaffChildBenefitUseCase.StudentCoverage;
import gn.lacaeducation.billing.domain.FeeType;
import gn.lacaeducation.billing.domain.StaffChildBenefitPolicy;
import gn.lacaeducation.common.TenantContext;
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
 * Politique de prise en charge des enfants du personnel (§6), paramétrable, et calcul de la prise en
 * charge applicable à un élève. Configuration réservée à la direction.
 */
@Tag(name = "Staff Child Benefits", description = "Prise en charge des enfants du personnel (§6)")
@RestController
@RequestMapping("/api/v1/billing/staff-child-benefits")
@RequiredArgsConstructor
public class StaffChildBenefitController {

    private final StaffChildBenefitUseCase useCase;

    @Operation(summary = "Créer une politique de prise en charge des enfants du personnel")
    @PreAuthorize("hasAuthority('avantage_personnel:create')")
    @PostMapping
    public ResponseEntity<StaffChildBenefitPolicy> create(@RequestBody PolicyCommand cmd) {
        StaffChildBenefitPolicy p = useCase.create(TenantContext.get(), cmd);
        return ResponseEntity.created(URI.create("/api/v1/billing/staff-child-benefits/" + p.getId())).body(p);
    }

    @Operation(summary = "Lister les politiques du tenant")
    @PreAuthorize("hasAuthority('avantage_personnel:read')")
    @GetMapping
    public List<StaffChildBenefitPolicy> list() {
        return useCase.findByTenant(TenantContext.get());
    }

    @Operation(summary = "Modifier une politique")
    @PreAuthorize("hasAuthority('avantage_personnel:update')")
    @PutMapping("/{id}")
    public StaffChildBenefitPolicy update(@PathVariable Long id, @RequestBody PolicyCommand cmd) {
        return useCase.update(id, cmd);
    }

    @Operation(summary = "Supprimer une politique")
    @PreAuthorize("hasAuthority('avantage_personnel:delete')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        useCase.delete(id);
    }

    // Lecture ouverte aussi au comptable : avantage_personnel:read, accordé en lecture seule — sert aux
    // badges de la liste des élèves, que le comptable doit voir sans avoir la main sur la configuration
    // des politiques (avantage_personnel:create/update/delete ci-dessus).
    @Operation(summary = "Prise en charge école applicable à un élève (enfant d'employé) pour un frais")
    @PreAuthorize("hasAuthority('avantage_personnel:read')")
    @GetMapping("/coverage")
    public StaffCoverage coverage(@RequestParam Long studentId, @RequestParam FeeType feeType) {
        return useCase.computeCoverage(TenantContext.get(), studentId, feeType);
    }

    @Operation(summary = "Parmi une liste d'élèves, ceux pris en charge (enfants de personnel) pour un frais (pour badge dans la liste des élèves)")
    @PreAuthorize("hasAuthority('avantage_personnel:read')")
    @GetMapping("/coverage-batch")
    public List<StudentCoverage> coverageBatch(@RequestParam List<Long> studentIds, @RequestParam FeeType feeType) {
        return useCase.computeCoverageBatch(TenantContext.get(), studentIds, feeType);
    }

    @Operation(summary = "Parmi une liste d'élèves, ceux enfants du personnel SANS remise (scolarité prélevée sur salaire — pour badge distinct dans la liste des élèves)")
    @PreAuthorize("hasAuthority('avantage_personnel:read')")
    @GetMapping("/uncovered-batch")
    public List<Long> uncoveredBatch(@RequestParam List<Long> studentIds) {
        return useCase.findUncoveredStaffChildIds(TenantContext.get(), studentIds);
    }
}
