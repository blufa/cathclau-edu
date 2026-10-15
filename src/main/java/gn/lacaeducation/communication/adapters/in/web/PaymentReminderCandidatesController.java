package gn.lacaeducation.communication.adapters.in.web;

import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.communication.application.port.out.PaymentStatusListPdfGenerator;
import gn.lacaeducation.communication.application.service.PaymentReminderCandidatesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

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
    private final PaymentStatusListPdfGenerator pdfGenerator;

    @Operation(summary = "Lister les élèves (d'une classe, ou de toutes si classId omis) avec leur statut de paiement "
            + "(à jour/retard/échéance proche) et leurs tranches payées")
    @PreAuthorize("hasAuthority('communication:send')")
    @GetMapping
    public List<PaymentReminderCandidatesService.Candidate> list(@RequestParam(required = false) Long classId) {
        return classId != null ? service.listByClass(TenantContext.get(), classId) : service.listByTenant(TenantContext.get());
    }

    public record PrintRequest(Long classId, Set<Long> studentIds) {}

    @Operation(summary = "Imprimer la liste (telle que filtrée à l'écran) des élèves sélectionnés avec leur statut de paiement")
    @PreAuthorize("hasAuthority('communication:send')")
    @PostMapping(value = "/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> printPdf(@RequestBody PrintRequest req) {
        Long tenantId = TenantContext.get();
        List<PaymentReminderCandidatesService.Candidate> all = req.classId() != null
                ? service.listByClass(tenantId, req.classId()) : service.listByTenant(tenantId);
        List<PaymentReminderCandidatesService.Candidate> selected = all.stream()
                .filter(c -> req.studentIds().contains(c.studentId()))
                .toList();
        byte[] pdf = pdfGenerator.generate(tenantId, selected);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=suivi-paiements.pdf")
                .body(pdf);
    }
}
