package gn.lacaeducation.students.adapters.in.web;

import gn.lacaeducation.students.application.port.in.EnrollmentUseCase;
import gn.lacaeducation.students.application.port.in.EnrollmentUseCase.EnrollCommand;
import gn.lacaeducation.students.domain.Enrollment;
import gn.lacaeducation.students.domain.EnrollmentStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@Tag(name = "Enrollments", description = "Inscriptions et réinscriptions, avec circuit de validation")
@RestController
@RequestMapping("/api/v1/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentUseCase useCase;

    @Operation(summary = "Inscrire un élève dans une classe pour une année scolaire")
    @PostMapping
    @PreAuthorize("hasAuthority('inscription:create')")
    public ResponseEntity<Enrollment> enroll(@Valid @RequestBody EnrollCommand cmd) {
        Enrollment e = useCase.enroll(cmd);
        return ResponseEntity.created(URI.create("/api/v1/enrollments/" + e.getId())).body(e);
    }

    @Operation(summary = "Réinscrire un élève (année scolaire suivante)")
    @PostMapping("/re-enrollment")
    @PreAuthorize("hasAuthority('inscription:create')")
    public ResponseEntity<Enrollment> reenroll(@Valid @RequestBody EnrollCommand cmd) {
        Enrollment e = useCase.reenroll(cmd);
        return ResponseEntity.created(URI.create("/api/v1/enrollments/" + e.getId())).body(e);
    }

    @Operation(summary = "Modifier une inscription en attente (classe / année scolaire / type)")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('inscription:create')")
    public Enrollment update(@PathVariable Long id, @Valid @RequestBody EnrollCommand cmd) {
        return useCase.update(id, cmd);
    }

    @Operation(summary = "Rechercher des inscriptions par élève, classe, année scolaire et/ou statut")
    @GetMapping
    @PreAuthorize("hasAuthority('inscription:read')")
    public List<Enrollment> search(@RequestParam(required = false) Long studentId,
                                   @RequestParam(required = false) Long classId,
                                   @RequestParam(required = false) Long academicYearId,
                                   @RequestParam(required = false) EnrollmentStatus status) {
        return useCase.search(studentId, classId, academicYearId, status);
    }

    @Operation(summary = "Détail d'une inscription")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('inscription:read')")
    public Enrollment findById(@PathVariable Long id) {
        return useCase.findById(id);
    }

    @Operation(summary = "Dette (factures impayées d'une année antérieure) de chaque élève listé — "
            + "pour afficher/bloquer en amont sur l'écran de réinscription en masse")
    @GetMapping("/arrears")
    @PreAuthorize("hasAuthority('inscription:read')")
    public List<EnrollmentUseCase.ArrearsSummary> arrears(@RequestParam List<Long> studentIds,
                                                           @RequestParam Long academicYearId) {
        return useCase.arrears(studentIds, academicYearId);
    }

    @Operation(summary = "Modifier le choix d'accès à la plateforme pour cet élève (opt-in du parent, modifiable en cours d'année)")
    @PatchMapping("/{id}/platform-access")
    @PreAuthorize("hasAuthority('inscription:create')")
    public Enrollment updatePlatformAccess(@PathVariable Long id, @RequestBody PlatformAccessRequest r) {
        return useCase.updatePlatformAccessOptIn(id, Boolean.TRUE.equals(r.optIn()));
    }

    public record PlatformAccessRequest(Boolean optIn) {}

    @Operation(summary = "Modifier le choix de paiement mensuel pour cet élève (opt-in du parent, modifiable "
            + "en cours d'année) — les tranches multi-mois de sa classe sont alors facturées mois par mois")
    @PatchMapping("/{id}/monthly-payment")
    @PreAuthorize("hasAuthority('inscription:create')")
    public Enrollment updateMonthlyPayment(@PathVariable Long id, @RequestBody MonthlyPaymentRequest r) {
        return useCase.updateMonthlyPaymentOptIn(id, Boolean.TRUE.equals(r.optIn()));
    }

    public record MonthlyPaymentRequest(Boolean optIn) {}

    @Operation(summary = "Valider une inscription en attente (vérifie la capacité et l'unicité par année) — "
            + "overrideCapacity=true force la validation malgré une classe déjà à sa capacité maximale "
            + "(§6 cahier des charges : jamais silencieux, consigné dans le journal d'audit)")
    @PatchMapping("/{id}/validate")
    @PreAuthorize("hasAuthority('inscription:validate')")
    public Enrollment validate(@PathVariable Long id,
                                @RequestParam(defaultValue = "false") boolean overrideCapacity,
                                @AuthenticationPrincipal Jwt jwt) {
        return useCase.validate(id, overrideCapacity, currentUserId(jwt));
    }

    @Operation(summary = "Annuler une inscription")
    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('inscription:validate')")
    public Enrollment cancel(@PathVariable Long id) {
        return useCase.cancel(id);
    }

    @Operation(summary = "Marquer une inscription validée comme abandon (élève parti en cours d'année) — "
            + "contrairement à l'annulation, les factures déjà émises continuent de compter dans le recouvrement de la classe")
    @PatchMapping("/{id}/abandon")
    @PreAuthorize("hasAuthority('inscription:validate')")
    public Enrollment abandon(@PathVariable Long id) {
        return useCase.abandon(id);
    }

    @Operation(summary = "Transférer une inscription validée vers une autre classe en cours d'année — "
            + "si les frais scolaires diffèrent, les mensualités impayées de l'ancienne classe sont annulées "
            + "et ce qui a déjà été payé est reporté en crédit sur les nouvelles mensualités")
    @PatchMapping("/{id}/transfer-class")
    @PreAuthorize("hasAuthority('inscription:validate')")
    public Enrollment transferClass(@PathVariable Long id, @RequestBody TransferClassRequest r,
                                     @AuthenticationPrincipal Jwt jwt) {
        return useCase.transferClass(id, r.newClassId(), Boolean.TRUE.equals(r.overrideCapacity()), currentUserId(jwt));
    }

    public record TransferClassRequest(Long newClassId, Boolean overrideCapacity) {}

    private Long currentUserId(Jwt jwt) {
        if (jwt == null) return null;
        try { return Long.parseLong(jwt.getSubject()); } catch (RuntimeException e) { return null; }
    }

    @Operation(summary = "Supprimer une inscription")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('inscription:validate')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        useCase.delete(id);
    }
}
