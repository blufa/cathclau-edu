package gn.lacaeducation.portal.adapters.in.web;

import gn.lacaeducation.academics.AcademicsDataProvider;
import gn.lacaeducation.billing.StudentBillingProvider;
import gn.lacaeducation.grading.StudentReportProvider;
import gn.lacaeducation.students.ParentAccessProvider;
import gn.lacaeducation.students.StudentDataProvider;
import gn.lacaeducation.students.StudentPublicInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/**
 * Tableau de bord du portail parent (§1) : agrège, pour chaque enfant du parent connecté, sa classe,
 * sa moyenne générale (dernier bulletin publié) et le solde de ses frais. Lecture seule et
 * strictement cloisonné aux enfants rattachés au compte.
 */
@Tag(name = "Parent Portal", description = "Tableau de bord agrégé du parent (lecture seule, scopé à ses enfants)")
@RestController
@RequestMapping("/api/v1/parent")
@RequiredArgsConstructor
public class ParentDashboardController {

    private final ParentAccessProvider parentAccessProvider;
    private final StudentDataProvider studentDataProvider;
    private final AcademicsDataProvider academicsDataProvider;
    private final StudentReportProvider studentReportProvider;
    private final StudentBillingProvider studentBillingProvider;

    public record ChildSummary(Long studentId, String firstName, String lastName, String className,
                               BigDecimal overallAverage, Integer rank, String mention,
                               BigDecimal feesRemaining) {}

    public record ParentDashboard(int childrenCount, List<ChildSummary> children) {}

    @Operation(summary = "Tableau de bord du parent connecté : ses enfants avec classe, moyenne et solde des frais")
    @PreAuthorize("hasRole('PARENT')")
    @GetMapping("/dashboard")
    public ParentDashboard dashboard(@AuthenticationPrincipal Jwt jwt) {
        Long userId = currentUserId(jwt);
        List<ChildSummary> children = parentAccessProvider.findChildStudentIds(userId).stream()
                .map(this::summarize)
                .filter(java.util.Objects::nonNull)
                .toList();
        return new ParentDashboard(children.size(), children);
    }

    private ChildSummary summarize(Long studentId) {
        StudentPublicInfo student = studentDataProvider.findStudent(studentId).orElse(null);
        if (student == null) return null;

        String className = studentDataProvider.findCurrentClassId(studentId)
                .flatMap(academicsDataProvider::findClass)
                .map(c -> c.label())
                .orElse(null);

        StudentReportProvider.ReportInfo report = studentReportProvider.latestPublished(studentId).orElse(null);
        BigDecimal average = report != null ? report.average() : null;
        Integer rank = report != null ? report.rank() : null;
        String mention = report != null ? report.mention() : null;

        BigDecimal remaining = studentBillingProvider.latestInvoice(studentId)
                .map(StudentBillingProvider.InvoiceInfo::remaining)
                .orElse(null);

        return new ChildSummary(studentId, student.firstName(), student.lastName(), className,
                average, rank, mention, remaining);
    }

    private Long currentUserId(Jwt jwt) {
        if (jwt == null) return null;
        try { return Long.parseLong(jwt.getSubject()); } catch (RuntimeException e) { return null; }
    }
}
