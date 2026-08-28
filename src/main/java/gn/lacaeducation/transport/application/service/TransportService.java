package gn.lacaeducation.transport.application.service;

import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.transport.application.event.TransportSubscriptionCreatedEvent;
import gn.lacaeducation.transport.application.port.out.TransportRouteRepository;
import gn.lacaeducation.transport.application.port.out.TransportStopRepository;
import gn.lacaeducation.transport.application.port.out.TransportSubscriptionRepository;
import gn.lacaeducation.transport.domain.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Transport scolaire (inspiré OpenEduCat) : lignes (avec véhicule/chauffeur), arrêts et abonnements
 * des élèves. Module autonome qui ne référence les élèves que par identifiant ; la facturation passe
 * par le module billing existant (type de frais TRANSPORT).
 */
@Service
@Transactional
@RequiredArgsConstructor
public class TransportService {

    private final TransportRouteRepository routeRepo;
    private final TransportStopRepository stopRepo;
    private final TransportSubscriptionRepository subRepo;
    private final ApplicationEventPublisher eventPublisher;

    // ── Lignes ───────────────────────────────────────────────────────────────

    public record RouteCommand(Long tenantId, String name, String description, String vehicleRegistration,
                               String driverName, String driverPhone, Integer capacity, BigDecimal monthlyFee,
                               Boolean active) {}

    public TransportRoute createRoute(RouteCommand c) {
        if (c.name() == null || c.name().isBlank()) throw new BusinessRuleException("Le nom de la ligne est requis");
        return routeRepo.save(applyRoute(TransportRoute.builder().tenantId(c.tenantId()).build(), c));
    }

    public TransportRoute updateRoute(Long id, RouteCommand c) { return routeRepo.save(applyRoute(loadRoute(id), c)); }

    private TransportRoute applyRoute(TransportRoute r, RouteCommand c) {
        r.setName(c.name()); r.setDescription(c.description()); r.setVehicleRegistration(c.vehicleRegistration());
        r.setDriverName(c.driverName()); r.setDriverPhone(c.driverPhone()); r.setCapacity(c.capacity());
        r.setMonthlyFee(c.monthlyFee()); r.setActive(c.active() == null || c.active());
        return r;
    }

    public void deleteRoute(Long id) { routeRepo.deleteById(id); }

    @Transactional(readOnly = true)
    public List<TransportRoute> routes(Long tenantId) { return routeRepo.findByTenantId(tenantId); }

    @Transactional(readOnly = true)
    public TransportRoute route(Long id) { return loadRoute(id); }

    // ── Arrêts ───────────────────────────────────────────────────────────────

    public TransportStop addStop(Long tenantId, Long routeId, String name, LocalTime pickup, LocalTime drop, Integer sequence) {
        loadRoute(routeId);
        if (name == null || name.isBlank()) throw new BusinessRuleException("Le nom de l'arrêt est requis");
        return stopRepo.save(TransportStop.builder()
                .tenantId(tenantId).routeId(routeId).name(name).pickupTime(pickup).dropTime(drop)
                .sequence(sequence != null ? sequence : 10).build());
    }

    public void deleteStop(Long id) { stopRepo.deleteById(id); }

    @Transactional(readOnly = true)
    public List<TransportStop> stops(Long routeId) { return stopRepo.findByRouteIdOrderBySequenceAsc(routeId); }

    // ── Abonnements ───────────────────────────────────────────────────────────

    public record SubscribeCommand(Long tenantId, Long routeId, Long stopId, Long studentId,
                                   TransportDirection direction, LocalDate startDate, BigDecimal monthlyFee) {}

    public TransportSubscription subscribe(SubscribeCommand c) {
        TransportRoute route = loadRoute(c.routeId());
        if (c.studentId() == null) throw new BusinessRuleException("L'élève est requis");
        long active = subRepo.findByRouteId(c.routeId()).stream()
                .filter(s -> s.getStatus() == SubscriptionStatus.ACTIVE).count();
        if (route.getCapacity() != null && active >= route.getCapacity()) {
            throw new BusinessRuleException("La ligne a atteint sa capacité (" + route.getCapacity() + ")");
        }
        BigDecimal fee = c.monthlyFee() != null ? c.monthlyFee() : route.getMonthlyFee();
        TransportSubscription saved = subRepo.save(TransportSubscription.builder()
                .tenantId(c.tenantId()).routeId(c.routeId()).stopId(c.stopId()).studentId(c.studentId())
                .direction(c.direction() != null ? c.direction() : TransportDirection.BOTH)
                .startDate(c.startDate() != null ? c.startDate() : LocalDate.now())
                .monthlyFee(fee).status(SubscriptionStatus.ACTIVE).createdAt(LocalDateTime.now()).build());
        // La facturation passe par billing (frais TRANSPORT configuré avec ses échéances dans Frais
        // scolaires, comme la scolarité) : ce fait déclenche la génération des factures correspondantes,
        // pour qu'elles apparaissent tout de suite sous « frais annexes » à l'encaissement.
        eventPublisher.publishEvent(new TransportSubscriptionCreatedEvent(c.tenantId(), c.studentId(), c.routeId()));
        return saved;
    }

    public TransportSubscription cancel(Long id) {
        TransportSubscription s = subRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TransportSubscription", id));
        s.setStatus(SubscriptionStatus.CANCELLED);
        return subRepo.save(s);
    }

    public void deleteSubscription(Long id) { subRepo.deleteById(id); }

    @Transactional(readOnly = true)
    public List<TransportSubscription> subscriptions(Long tenantId, Long routeId, Long studentId) {
        if (routeId != null) return subRepo.findByRouteId(routeId);
        if (studentId != null) return subRepo.findByStudentId(studentId);
        return subRepo.findByTenantId(tenantId);
    }

    private TransportRoute loadRoute(Long id) {
        return routeRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("TransportRoute", id));
    }
}
