package gn.lacaeducation.communication.adapters.in.web;

import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.communication.application.port.in.SmsRetryQueueUseCase;
import gn.lacaeducation.communication.domain.SmsRetryEntry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * File d'attente de renvoi des SMS échoués (§retour direction — connexion serveur↔fournisseur SMS
 * instable) : consultation et relance manuelle, en plus du renvoi automatique planifié.
 */
@Tag(name = "SMS Retry Queue", description = "File d'attente de renvoi des SMS échoués vers le fournisseur")
@RestController
@RequestMapping("/api/v1/communications/sms-retry-queue")
@RequiredArgsConstructor
public class SmsRetryQueueController {

    private final SmsRetryQueueUseCase useCase;

    @Operation(summary = "Lister la file d'attente de renvoi SMS du tenant")
    @PreAuthorize("hasAuthority('communication:read')")
    @GetMapping
    public List<SmsRetryEntry> list() {
        return useCase.findByTenant(TenantContext.get());
    }

    @Operation(summary = "Relancer manuellement une entrée de la file (remet le compteur de tentatives à zéro)")
    @PreAuthorize("hasAuthority('communication:send')")
    @PostMapping("/{id}/rejouer")
    public SmsRetryEntry replay(@PathVariable Long id) {
        return useCase.replay(id);
    }
}
