package gn.lacaeducation.transport.adapters.in.web;

import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.transport.application.service.TransportService;
import gn.lacaeducation.transport.application.service.TransportService.RouteCommand;
import gn.lacaeducation.transport.domain.TransportRoute;
import gn.lacaeducation.transport.domain.TransportStop;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

@Tag(name = "Transport Routes", description = "Lignes de transport (véhicule/chauffeur) et arrêts")
@RestController
@RequestMapping("/api/v1/transport/routes")
@RequiredArgsConstructor
public class TransportRouteController {

    private final TransportService service;

    public record RouteRequest(String name, String description, String vehicleRegistration, String driverName,
                               String driverPhone, Integer capacity, BigDecimal monthlyFee, Boolean active) {}
    public record StopRequest(String name, LocalTime pickupTime, LocalTime dropTime, Integer sequence) {}

    @Operation(summary = "Lister les lignes")
    @GetMapping
    public List<TransportRoute> list() { return service.routes(TenantContext.get()); }

    @Operation(summary = "Détail d'une ligne")
    @GetMapping("/{id}")
    public TransportRoute get(@PathVariable Long id) { return service.route(id); }

    @Operation(summary = "Créer une ligne")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','SECRETARY')")
    @PostMapping
    public TransportRoute create(@RequestBody RouteRequest r) { return service.createRoute(toCommand(r)); }

    @Operation(summary = "Modifier une ligne")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','SECRETARY')")
    @PutMapping("/{id}")
    public TransportRoute update(@PathVariable Long id, @RequestBody RouteRequest r) { return service.updateRoute(id, toCommand(r)); }

    @Operation(summary = "Supprimer une ligne")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) { service.deleteRoute(id); return ResponseEntity.noContent().build(); }

    @Operation(summary = "Lister les arrêts d'une ligne")
    @GetMapping("/{id}/stops")
    public List<TransportStop> stops(@PathVariable Long id) { return service.stops(id); }

    @Operation(summary = "Ajouter un arrêt à une ligne")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','SECRETARY')")
    @PostMapping("/{id}/stops")
    public TransportStop addStop(@PathVariable Long id, @RequestBody StopRequest r) {
        return service.addStop(TenantContext.get(), id, r.name(), r.pickupTime(), r.dropTime(), r.sequence());
    }

    @Operation(summary = "Supprimer un arrêt")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','SECRETARY')")
    @DeleteMapping("/stops/{stopId}")
    public ResponseEntity<Void> deleteStop(@PathVariable Long stopId) { service.deleteStop(stopId); return ResponseEntity.noContent().build(); }

    private RouteCommand toCommand(RouteRequest r) {
        return new RouteCommand(TenantContext.get(), r.name(), r.description(), r.vehicleRegistration(),
                r.driverName(), r.driverPhone(), r.capacity(), r.monthlyFee(), r.active());
    }
}
