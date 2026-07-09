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

@Tag(name = "Student Cards", description = "Issue and manage student cards with signed QR")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class StudentCardController {

    private final StudentCardService service;

    public record IssueRequest(Long cardTemplateId, Long academicYearId, Long classId, String studentNumber,
                               String photoUrl, LocalDate expirationDate) {}

    @Operation(summary = "Issue a card for a student")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SCHOOL_ADMIN','DIRECTOR','SECRETARY')")
    @PostMapping("/eleves/{studentId}/carte")
    public StudentCard issue(@PathVariable Long studentId, @RequestBody IssueRequest r) {
        return service.issue(new IssueCardCommand(TenantContext.get(), studentId, r.cardTemplateId(),
                r.academicYearId(), r.classId(), r.studentNumber(), r.photoUrl(), r.expirationDate()));
    }

    @Operation(summary = "Current active card of a student")
    @GetMapping("/eleves/{studentId}/carte")
    public ResponseEntity<StudentCard> current(@PathVariable Long studentId) {
        return service.currentCard(studentId).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "List cards for the current tenant")
    @GetMapping("/cartes")
    public List<StudentCard> list() { return service.findByTenant(TenantContext.get()); }

    @Operation(summary = "Re-issue (replace) a card")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SCHOOL_ADMIN','DIRECTOR','SECRETARY')")
    @PatchMapping("/cartes/{id}/remplacer")
    public StudentCard replace(@PathVariable Long id) { return service.replace(id); }

    @Operation(summary = "Declare a card lost")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SCHOOL_ADMIN','DIRECTOR','SECRETARY')")
    @PatchMapping("/cartes/{id}/declarer-perdue")
    public StudentCard declareLost(@PathVariable Long id) { return service.declareLost(id); }

    @Operation(summary = "Verify a card by scanning its QR (payload + signature)")
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
