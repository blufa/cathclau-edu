package gn.lacaeducation.studentcards.adapters.in.web;

import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.studentcards.application.service.StudentCardService;
import gn.lacaeducation.studentcards.application.service.StudentCardService.IssueCardCommand;
import gn.lacaeducation.studentcards.domain.StudentCard;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Tag(name = "Student Cards", description = "Émettre et gérer les cartes scolaires avec QR code signé")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class StudentCardController {

    private final StudentCardService service;

    public record IssueRequest(Long cardTemplateId, Long academicYearId, Long classId, String studentNumber,
                               String photoUrl, LocalDate expirationDate) {}

    @Operation(summary = "Émettre une carte pour un élève")
    @PreAuthorize("hasAuthority('carte:manage')")
    @PostMapping("/eleves/{studentId}/carte")
    public StudentCard issue(@PathVariable Long studentId, @RequestBody IssueRequest r) {
        return service.issue(new IssueCardCommand(TenantContext.get(), studentId, r.cardTemplateId(),
                r.academicYearId(), r.classId(), r.studentNumber(), r.photoUrl(), r.expirationDate()));
    }

    @Operation(summary = "Carte active en cours d'un élève")
    @GetMapping("/eleves/{studentId}/carte")
    public ResponseEntity<StudentCard> current(@PathVariable Long studentId) {
        return service.currentCard(studentId).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Carte à jour d'un élève : émet si aucune n'existe, régénère automatiquement "
            + "si le matricule/la photo/la classe soumis diffère de la carte active, sinon renvoie "
            + "celle-ci inchangée — pour une émission en un clic depuis une liste (toujours à jour)")
    @PreAuthorize("hasAuthority('carte:manage')")
    @PostMapping("/eleves/{studentId}/carte/actuelle")
    public StudentCard getOrRefresh(@PathVariable Long studentId, @RequestBody IssueRequest r) {
        return service.getOrRefresh(new IssueCardCommand(TenantContext.get(), studentId, r.cardTemplateId(),
                r.academicYearId(), r.classId(), r.studentNumber(), r.photoUrl(), r.expirationDate()));
    }

    @Operation(summary = "Lister les cartes de l'établissement")
    @GetMapping("/cartes")
    public List<StudentCard> list() { return service.findByTenant(TenantContext.get()); }

    @Operation(summary = "Réémettre (remplacer) une carte")
    @PreAuthorize("hasAuthority('carte:manage')")
    @PatchMapping("/cartes/{id}/remplacer")
    public StudentCard replace(@PathVariable Long id) { return service.replace(id); }

    @Operation(summary = "Déclarer une carte perdue")
    @PreAuthorize("hasAuthority('carte:manage')")
    @PatchMapping("/cartes/{id}/declarer-perdue")
    public StudentCard declareLost(@PathVariable Long id) { return service.declareLost(id); }

    public record MarkPrintedRequest(List<Long> ids) {}

    @Operation(summary = "Marquer ces cartes comme imprimées (après passage sur une planche de badges)")
    @PreAuthorize("hasAuthority('carte:manage')")
    @PatchMapping("/cartes/marquer-imprimees")
    public List<StudentCard> markPrinted(@RequestBody MarkPrintedRequest r) { return service.markPrinted(r.ids()); }

    @Operation(summary = "Vérifier une carte en scannant son QR code (payload + signature)")
    @GetMapping("/cartes/verifier")
    public ResponseEntity<Map<String, Object>> verify(@RequestParam String payload, @RequestParam String signature) {
        return service.verify(payload, signature)
                .<ResponseEntity<Map<String, Object>>>map(c -> ResponseEntity.ok(Map.of(
                        "valid", true, "studentId", c.getStudentId(), "studentNumber",
                        c.getStudentNumber() != null ? c.getStudentNumber() : "", "cardNumber", c.getCardNumber(),
                        "status", c.getStatus().name())))
                .orElseGet(() -> ResponseEntity.ok(Map.of("valid", false)));
    }
}
