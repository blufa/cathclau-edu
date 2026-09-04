package gn.lacaeducation.grading.adapters.in.web;

import gn.lacaeducation.common.PortalAccess;
import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.grading.application.port.in.AnnualReportCardUseCase;
import gn.lacaeducation.grading.application.port.in.ReportCardUseCase;
import gn.lacaeducation.grading.domain.AnnualReportCard;
import gn.lacaeducation.grading.domain.ReportCard;
import gn.lacaeducation.grading.domain.ReportCardLine;
import gn.lacaeducation.grading.domain.ReportCardStatus;
import gn.lacaeducation.students.ParentAccessProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Report Cards", description = "Generate, publish, and query student report cards per period")
@RestController
@RequestMapping("/api/v1/grading/report-cards")
@RequiredArgsConstructor
public class ReportCardController {

    private final ReportCardUseCase useCase;
    private final AnnualReportCardUseCase annualUseCase;
    private final ParentAccessProvider parentAccessProvider;

    /**
     * Un parent restreint ne peut consulter/télécharger que les bulletins <b>publiés</b> de ses
     * propres enfants ; le personnel encadrant garde l'accès complet.
     */
    private void assertParentCanAccessReportCard(Jwt jwt, Long reportCardId) {
        if (jwt == null) return;
        List<String> roles = jwt.getClaimAsStringList("roles");
        if (!PortalAccess.isRestrictedParent(roles)) return;
        ReportCard rc = useCase.findById(reportCardId)
                .orElseThrow(() -> new AccessDeniedException("Bulletin introuvable."));
        Long userId;
        try { userId = Long.parseLong(jwt.getSubject()); } catch (RuntimeException e) { userId = null; }
        if (!parentAccessProvider.isParentOfStudent(userId, rc.getStudentId())
                || rc.getStatus() != ReportCardStatus.PUBLISHED) {
            throw new AccessDeniedException("Ce bulletin n'est pas accessible.");
        }
    }

    public record GenerateRequest(Long studentId, Long classId, Long periodId) {}
    public record GenerateBulkRequest(Long classId, Long periodId) {}

    // ── Generate ─────────────────────────────────────────────────────────────

    @Operation(summary = "Generate a report card for a single student in a given class + period. "
            + "Triggers full grade calculation including ranks.")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','SECRETARY')")
    @PostMapping("/generate")
    public ResponseEntity<ReportCard> generate(@RequestBody GenerateRequest r) {
        return ResponseEntity.ok(useCase.generate(
                new ReportCardUseCase.GenerateReportCardCommand(r.studentId(), r.classId(), r.periodId())));
    }

    @Operation(summary = "Generate report cards for ALL students in a class + period (bulk). "
            + "Replaces any existing report cards for that combination.")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','SECRETARY')")
    @PostMapping("/generate-bulk")
    public ResponseEntity<List<ReportCard>> generateBulk(@RequestBody GenerateBulkRequest r) {
        return ResponseEntity.ok(useCase.generateBulk(
                new ReportCardUseCase.GenerateBulkCommand(r.classId(), r.periodId())));
    }

    // ── Publish ───────────────────────────────────────────────────────────────

    @Operation(summary = "Publish a report card to make it visible to parents and students")
    @PreAuthorize("hasAuthority('bulletin:publish')")
    @PatchMapping("/{id}/publish")
    public ResponseEntity<ReportCard> publish(@PathVariable Long id) {
        return ResponseEntity.ok(useCase.publish(id));
    }

    @Operation(summary = "Ajuster l'appréciation du conseil de classe (Félicitations, Encouragements, Mise en garde…)")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','TEACHER')")
    @PatchMapping("/{id}/distinction")
    public ResponseEntity<ReportCard> setDistinction(@PathVariable Long id, @RequestParam gn.lacaeducation.grading.domain.Distinction distinction) {
        return ResponseEntity.ok(useCase.setDistinction(id, distinction));
    }

    // ── Query ─────────────────────────────────────────────────────────────────

    @Operation(summary = "Get a report card by ID (includes status, averages, rank, honor, decision)")
    @GetMapping("/{id}")
    public ResponseEntity<ReportCard> findById(@PathVariable Long id) {
        return useCase.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "List report cards with optional filters: classId, periodId, studentId")
    @GetMapping
    public List<ReportCard> findAll(@RequestParam(required = false) Long classId,
                                    @RequestParam(required = false) Long periodId,
                                    @RequestParam(required = false) Long studentId) {
        if (studentId != null && periodId != null) {
            return useCase.findByStudent(studentId, periodId).map(List::of).orElse(List.of());
        }
        if (studentId != null) {
            return useCase.findByStudentId(studentId);
        }
        if (classId != null && periodId != null) {
            return useCase.findByClass(classId, periodId);
        }
        return List.of();
    }

    @Operation(summary = "List all report cards for a class in a given period")
    @GetMapping("/class/{classId}/period/{periodId}")
    public List<ReportCard> findByClass(@PathVariable Long classId, @PathVariable Long periodId) {
        return useCase.findByClass(classId, periodId);
    }

    @Operation(summary = "Palmarès / tableau d'honneur d'une classe (PDF) : élèves classés par rang avec mention")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','RESPONSABLE_PEDAGOGIQUE','TEACHER','SECRETARY')")
    @GetMapping("/class/{classId}/period/{periodId}/palmares/download")
    public ResponseEntity<byte[]> honorRollDownload(@PathVariable Long classId, @PathVariable Long periodId) {
        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"palmares-classe-" + classId + "-periode-" + periodId + ".pdf\"")
                .body(useCase.classHonorRollPdf(classId, periodId));
    }

    @Operation(summary = "Centralisateur de classe (PDF) : une ligne par élève, colonnes Cours/Composition par matière")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','RESPONSABLE_PEDAGOGIQUE','TEACHER','SECRETARY')")
    @GetMapping("/class/{classId}/period/{periodId}/centralisateur/download")
    public ResponseEntity<byte[]> centralizerDownload(@PathVariable Long classId, @PathVariable Long periodId) {
        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"centralisateur-classe-" + classId + "-periode-" + periodId + ".pdf\"")
                .body(useCase.classCentralizerPdf(classId, periodId));
    }

    @Operation(summary = "Get the report card for a student in a given period")
    @GetMapping("/student/{studentId}/period/{periodId}")
    public ResponseEntity<ReportCard> findByStudent(@PathVariable Long studentId, @PathVariable Long periodId,
                                                    @AuthenticationPrincipal Jwt jwt) {
        boolean restrictedParent = jwt != null && PortalAccess.isRestrictedParent(jwt.getClaimAsStringList("roles"));
        if (jwt != null) {
            Long userId;
            try { userId = Long.parseLong(jwt.getSubject()); } catch (RuntimeException e) { userId = null; }
            PortalAccess.assertParentCanAccessStudent(jwt.getClaimAsStringList("roles"), userId, studentId,
                    parentAccessProvider::isParentOfStudent);
        }
        return useCase.findByStudent(studentId, periodId)
                // Un parent ne voit que les bulletins publiés.
                .filter(rc -> !restrictedParent || rc.getStatus() == ReportCardStatus.PUBLISHED)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get the report card PDF URL (triggers async generation if not yet available)")
    @GetMapping("/{id}/pdf")
    public ResponseEntity<Map<String, String>> pdf(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        assertParentCanAccessReportCard(jwt, id);
        return useCase.getPdf(id)
                .map(url -> ResponseEntity.ok(Map.of("url", url)))
                .orElseGet(() -> { useCase.generatePdfAsync(id); return ResponseEntity.accepted().body(Map.of("status", "GENERATING")); });
    }

    @Operation(summary = "Stream the report card PDF bytes (inline) — independent of Cloudinary delivery")
    @GetMapping("/{id}/pdf/download")
    public ResponseEntity<byte[]> pdfDownload(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        assertParentCanAccessReportCard(jwt, id);
        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"bulletin-" + id + ".pdf\"")
                .body(useCase.pdfBytes(id));
    }

    @Operation(summary = "Get the subject-by-subject detail lines of a report card")
    @GetMapping("/{id}/lines")
    public List<ReportCardLine> findLines(@PathVariable Long id) {
        return useCase.findLines(id);
    }

    @Operation(summary = "Delete a report card")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        useCase.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ── Bulletin annuel (agrégation des 2 semestres, §documents ministère) ──────

    @Operation(summary = "Bulletins annuels de tous les élèves d'une classe (1er + 2ème semestre), classés par rang")
    @GetMapping("/annual")
    public List<AnnualReportCard> annualByClass(@RequestParam Long classId, @RequestParam Long academicYearId) {
        return annualUseCase.findByClass(TenantContext.get(), classId, academicYearId);
    }

    @Operation(summary = "Bulletin annuel d'un élève")
    @GetMapping("/annual/{studentId}")
    public ResponseEntity<AnnualReportCard> annualByStudent(@PathVariable Long studentId,
                                                            @RequestParam Long classId, @RequestParam Long academicYearId) {
        return annualUseCase.findByStudent(TenantContext.get(), studentId, classId, academicYearId)
                .map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "PDF du bulletin annuel officiel d'un élève")
    @GetMapping("/annual/{studentId}/pdf/download")
    public ResponseEntity<byte[]> annualPdfDownload(@PathVariable Long studentId,
                                                     @RequestParam Long classId, @RequestParam Long academicYearId) {
        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"bulletin-annuel-" + studentId + ".pdf\"")
                .body(annualUseCase.pdfBytes(TenantContext.get(), studentId, classId, academicYearId));
    }
}
