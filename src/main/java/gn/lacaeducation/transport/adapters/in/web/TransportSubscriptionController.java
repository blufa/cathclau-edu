package gn.lacaeducation.transport.adapters.in.web;

import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.transport.application.service.TransportService;
import gn.lacaeducation.transport.application.service.TransportService.SubscribeCommand;
import gn.lacaeducation.transport.domain.TransportDirection;
import gn.lacaeducation.transport.domain.TransportSubscription;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Tag(name = "Transport Subscriptions", description = "Abonnements des élèves au transport")
@RestController
@RequestMapping("/api/v1/transport/subscriptions")
@RequiredArgsConstructor
public class TransportSubscriptionController {

    private final TransportService service;

    public record SubscribeRequest(Long routeId, Long stopId, Long studentId, TransportDirection direction,
                                   LocalDate startDate, BigDecimal monthlyFee) {}

    @Operation(summary = "Lister les abonnements (filtres ligne / élève)")
    @GetMapping
    public List<TransportSubscription> list(@RequestParam(required = false) Long routeId,
                                            @RequestParam(required = false) Long studentId) {
        return service.subscriptions(TenantContext.get(), routeId, studentId);
    }

    @Operation(summary = "Élèves ayant déjà réglé au moins un mois de transport (§retour comptable : "
            + "\"savoir qui a payé le transport\") — parmi les abonnés, ceux absents de cette liste n'ont encore rien payé")
    @GetMapping("/paid-student-ids")
    public List<Long> paidStudentIds() {
        return service.paidStudentIds(TenantContext.get());
    }

    // Action courante du comptable/secrétaire à l'inscription ou à l'encaissement (choisir le bus) —
    // jamais la configuration des lignes/arrêts (transport:manage, restée admin/direction/secrétaire,
    // déjà détentrice de transport:manage depuis V149). abonnement_transport:create est un privilège
    // plus étroit, accordé en plus pour ce seul geste.
    @Operation(summary = "Abonner un élève à une ligne")
    @PreAuthorize("hasAnyAuthority('transport:manage', 'abonnement_transport:create')")
    @PostMapping
    public TransportSubscription subscribe(@RequestBody SubscribeRequest r) {
        return service.subscribe(new SubscribeCommand(TenantContext.get(), r.routeId(), r.stopId(), r.studentId(),
                r.direction(), r.startDate(), r.monthlyFee()));
    }

    @Operation(summary = "Résilier un abonnement")
    @PreAuthorize("hasAuthority('transport:manage')")
    @PatchMapping("/{id}/cancel")
    public TransportSubscription cancel(@PathVariable Long id) { return service.cancel(id); }

    @Operation(summary = "Supprimer un abonnement")
    @PreAuthorize("hasAuthority('transport:manage')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) { service.deleteSubscription(id); return ResponseEntity.noContent().build(); }
}
