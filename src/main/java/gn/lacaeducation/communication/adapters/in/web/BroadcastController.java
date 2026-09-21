package gn.lacaeducation.communication.adapters.in.web;

import gn.lacaeducation.communication.application.port.in.BroadcastUseCase;
import gn.lacaeducation.communication.domain.BroadcastAudience;
import gn.lacaeducation.communication.domain.Communication;
import gn.lacaeducation.communication.domain.CommunicationChannel;
import gn.lacaeducation.communication.domain.CommunicationReason;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Diffusion de masse segmentée (§11.1) : résout automatiquement les destinataires réels selon
 * l'audience choisie (tous les parents, une classe, les garçons/filles d'une classe, tous les
 * enseignants, ou le(s) parent(s) d'un élève précis) — pas de sélection manuelle d'IDs.
 */
@Tag(name = "Communications — Diffusion", description = "Diffusion de masse segmentée aux parents/enseignants")
@RestController
@RequestMapping("/api/v1/communications/diffusion")
@RequiredArgsConstructor
public class BroadcastController {

    private final BroadcastUseCase broadcastUseCase;

    public record DiffusionRequest(Long tenantId, Long senderId, CommunicationChannel channel,
                                   BroadcastAudience audience, Long classId, Long studentId,
                                   String subject, String content, CommunicationReason reason) {}

    @Operation(summary = "Diffuser un message à une audience segmentée (parents, classe, sexe, enseignants, élève)")
    @PreAuthorize("hasAuthority('communication:send')")
    @PostMapping
    public ResponseEntity<Communication> broadcast(@RequestBody DiffusionRequest r) {
        Communication result = broadcastUseCase.broadcast(new BroadcastUseCase.BroadcastCommand(
                r.tenantId(), r.senderId(), r.channel(), r.audience(), r.classId(), r.studentId(),
                r.subject(), r.content(), r.reason()));
        if (result == null) {
            return ResponseEntity.unprocessableEntity().build();
        }
        return ResponseEntity.ok(result);
    }
}
