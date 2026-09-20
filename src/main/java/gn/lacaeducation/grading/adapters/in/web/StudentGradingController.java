package gn.lacaeducation.grading.adapters.in.web;

import gn.lacaeducation.grading.application.port.in.GradeUseCase;
import gn.lacaeducation.grading.application.port.in.ReportCardUseCase;
import gn.lacaeducation.grading.domain.Grade;
import gn.lacaeducation.grading.domain.ReportCard;
import gn.lacaeducation.common.PortalAccess;
import gn.lacaeducation.students.ParentAccessProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Student-centric grading endpoints.
 * Exposes grade and report-card queries under /api/v1/students/{id}/...
 * so that student and parent portals get a clean, resource-oriented URL.
 */
@Tag(name = "Student Grading", description = "Vue des notes et bulletins centrée sur l'élève")
@RestController
@RequestMapping("/api/v1/students/{studentId}")
@RequiredArgsConstructor
public class StudentGradingController {

    private final GradeUseCase gradeUseCase;
    private final ReportCardUseCase reportCardUseCase;
    private final ParentAccessProvider parentAccessProvider;

    @Operation(summary = "Lister toutes les notes d'un élève, filtrable par periodId")
    @GetMapping("/grades")
    public List<Grade> getGrades(@PathVariable Long studentId,
                                  @RequestParam(required = false) Long periodId,
                                  @AuthenticationPrincipal Jwt jwt) {
        assertParentScope(jwt, studentId);
        if (periodId != null) {
            return gradeUseCase.findByStudentAndPeriod(studentId, periodId);
        }
        return gradeUseCase.findByStudent(studentId);
    }

    @Operation(summary = "Lister les bulletins publiés d'un élève, toutes périodes confondues")
    @GetMapping("/report-cards")
    public List<ReportCard> getReportCards(@PathVariable Long studentId, @AuthenticationPrincipal Jwt jwt) {
        assertParentScope(jwt, studentId);
        return reportCardUseCase.findByStudentId(studentId);
    }

    /**
     * Un compte parent (sans rôle d'encadrement) ne peut consulter que les élèves qui lui sont
     * rattachés ; le personnel garde l'accès complet.
     */
    private void assertParentScope(Jwt jwt, Long studentId) {
        if (jwt == null) return;
        Long userId;
        try { userId = Long.parseLong(jwt.getSubject()); } catch (RuntimeException e) { userId = null; }
        PortalAccess.assertParentCanAccessStudent(jwt.getClaimAsStringList("roles"), userId, studentId,
                parentAccessProvider::isParentOfStudent);
    }
}
