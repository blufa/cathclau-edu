package gn.lacaeducation.communication.adapters.in.web;

import gn.lacaeducation.communication.application.port.in.ScheduledBroadcastUseCase;
import gn.lacaeducation.communication.domain.BroadcastAudience;
import gn.lacaeducation.communication.domain.CommunicationChannel;
import gn.lacaeducation.communication.domain.CommunicationReason;
import gn.lacaeducation.communication.domain.ScheduledBroadcast;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Diffusion de masse PROGRAMMÉE (§11.1 bonus) : au lieu d'envoyer immédiatement, le message part
 * automatiquement à la date/heure choisie (typiquement liée à un événement école) — sans intervention
 * manuelle le jour J.
 */
@Tag(name = "Communications — Diffusion programmée", description = "Programmation d'une diffusion de masse à une date/heure future")
@RestController
@RequestMapping("/api/v1/communications/diffusion/programmee")
@RequiredArgsConstructor
public class ScheduledBroadcastController {

    private final ScheduledBroadcastUseCase useCase;

    public record ScheduleRequest(Long tenantId, Long senderId, CommunicationChannel channel,
                                  BroadcastAudience audience, Long classId, Long studentId,
                                  String subject, String content, CommunicationReason reason,
                                  LocalDateTime sendAt, Long schoolEventId) {}

    @Operation(summary = "Programmer une diffusion de masse à une date/heure future (audience segmentée, y compris « toute l'école »)")
    @PreAuthorize("hasAuthority('communication:send')")
    @PostMapping
    public ResponseEntity<ScheduledBroadcast> schedule(@RequestBody ScheduleRequest r) {
        ScheduledBroadcast created = useCase.schedule(new ScheduledBroadcastUseCase.ScheduleCommand(
                r.tenantId(), r.senderId(), r.channel(), r.audience(), r.classId(), r.studentId(),
                r.subject(), r.content(), r.reason(), r.sendAt(), r.schoolEventId()));
        return ResponseEntity.created(URI.create("/api/v1/communications/diffusion/programmee/" + created.getId())).body(created);
    }

    @Operation(summary = "Lister les diffusions programmées d'un tenant (en attente, envoyées, annulées, échouées)")
    @PreAuthorize("hasAuthority('communication:send')")
    @GetMapping
    public List<ScheduledBroadcast> findByTenant(@RequestParam Long tenantId) {
        return useCase.findByTenant(tenantId);
    }

    @Operation(summary = "Annuler une diffusion programmée tant qu'elle n'est pas encore partie")
    @PreAuthorize("hasAuthority('communication:send')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(@PathVariable Long id) {
        useCase.cancel(id);
        return ResponseEntity.noContent().build();
    }
}
