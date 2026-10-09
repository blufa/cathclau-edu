package gn.lacaeducation.communication.adapters.in.web;

import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.communication.application.service.PaymentReminderCandidatesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Pour une classe donnée, tous ses élèves avec leur statut de paiement (à jour / en retard / échéance
 * proche) de la mensualité et le nombre de tranches déjà réglées, avec les contacts de leurs parents —
 * pour trier/filtrer avant d'envoyer une relance SMS groupée (voir aussi
 * {@code BroadcastAudience.OVERDUE_PAYERS}/{@code UPCOMING_PAYERS} pour un envoi direct à tout
 * l'établissement sans passer par cette liste classe par classe).
 */
@Tag(name = "Communications — Relances paiement", description = "Liste triable par classe du statut de paiement des élèves, pour cibler une relance SMS")
@RestController
@RequestMapping("/api/v1/communications/payment-reminders")
@RequiredArgsConstructor
public class PaymentReminderCandidatesController {

    private final PaymentReminderCandidatesService service;

    @Operation(summary = "Lister les élèves d'une classe avec leur statut de paiement (à jour/retard/échéance proche) et leurs tranches payées")
    @PreAuthorize("hasAuthority('communication:send')")
    @GetMapping
    public List<PaymentReminderCandidatesService.Candidate> listByClass(@RequestParam Long classId) {
        return service.listByClass(TenantContext.get(), classId);
    }
}
