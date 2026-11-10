package gn.lacaeducation.grading.adapters.in.web;

import gn.lacaeducation.grading.application.service.MonthlyReportCardService;
import gn.lacaeducation.grading.domain.MonthlyReportCard;
import gn.lacaeducation.grading.domain.MonthlyReportCardLine;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;

/**
 * Bulletin de contrôle mensuel (§censeur) : diagnostic de niveau en cours d'année, totalement
 * indépendant du bulletin officiel ({@code ReportCardController}) — mêmes permissions
 * (bulletin:generate/bulletin:read), pas de nouvelle permission à créer pour ce sous-domaine plus
 * simple (pas de publication, pas de rang/mention/décision).
 */
@Tag(name = "Monthly Report Cards", description = "Bulletin de contrôle mensuel (diagnostic de niveau en cours d'année)")
@RestController
@RequestMapping("/api/v1/grading/monthly-report-cards")
@RequiredArgsConstructor
public class MonthlyReportCardController {

    private final MonthlyReportCardService service;

    public record GenerateRequest(Long studentId, Long classId, String yearMonth) {}
    public record GenerateBulkRequest(Long classId, String yearMonth) {}
    public record CommentRequest(String comment) {}

    @Operation(summary = "Générer le bulletin mensuel d'un seul élève pour une classe et un mois donnés")
    @PreAuthorize("hasAuthority('bulletin:generate')")
    @PostMapping("/generate")
    public MonthlyReportCard generate(@RequestBody GenerateRequest r) {
        return service.generateForStudent(r.studentId(), r.classId(), YearMonth.parse(r.yearMonth()));
    }

    @Operation(summary = "Générer les bulletins mensuels de tous les élèves ayant au moins une note "
            + "d'évaluation mensuelle ce mois-ci, pour une classe. Remplace les bulletins existants pour "
            + "cette combinaison (élève, classe, mois) — l'appréciation du censeur déjà saisie est conservée.")
    @PreAuthorize("hasAuthority('bulletin:generate')")
    @PostMapping("/generate-bulk")
    public List<MonthlyReportCard> generateBulk(@RequestBody GenerateBulkRequest r) {
        return service.generateForClass(r.classId(), YearMonth.parse(r.yearMonth()));
    }

    @Operation(summary = "Ajuster l'appréciation manuelle du censeur sur un bulletin mensuel déjà généré")
    @PreAuthorize("hasAuthority('bulletin:generate')")
    @PatchMapping("/{id}/comment")
    public MonthlyReportCard setComment(@PathVariable Long id, @RequestBody CommentRequest r) {
        return service.setComment(id, r.comment());
    }

    @Operation(summary = "Consulter un bulletin mensuel par son identifiant")
    @GetMapping("/{id}")
    public ResponseEntity<MonthlyReportCard> findById(@PathVariable Long id) {
        return service.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Lister les bulletins mensuels d'une classe pour un mois donné")
    @GetMapping
    public List<MonthlyReportCard> findByClass(@RequestParam Long classId, @RequestParam String yearMonth) {
        return service.findByClass(classId, YearMonth.parse(yearMonth));
    }

    @Operation(summary = "Détail par matière d'un bulletin mensuel")
    @GetMapping("/{id}/lines")
    public List<MonthlyReportCardLine> findLines(@PathVariable Long id) {
        return service.findLines(id);
    }

    @Operation(summary = "Flux des octets du PDF du bulletin mensuel")
    @PreAuthorize("hasAuthority('bulletin:read')")
    @GetMapping("/{id}/pdf/download")
    public ResponseEntity<byte[]> pdfDownload(@PathVariable Long id) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"bulletin-mensuel-" + id + ".pdf\"")
                .body(service.pdfBytes(id));
    }
}
