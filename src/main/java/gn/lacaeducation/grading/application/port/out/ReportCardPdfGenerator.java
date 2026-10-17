package gn.lacaeducation.grading.application.port.out;

import gn.lacaeducation.grading.domain.ReportCard;
import gn.lacaeducation.grading.domain.ReportCardLine;
import gn.lacaeducation.grading.domain.SubjectMonthlyBreakdown;

import java.util.List;
import java.util.Map;

/** Renders a report card (branded header + per-subject lines + summary + signatures) to PDF bytes. */
public interface ReportCardPdfGenerator {

    byte[] generate(ReportCard reportCard, List<ReportCardLine> lines, ReportCardContext context);

    /**
     * Human-readable context resolved from other modules so the document shows names — never raw IDs.
     * Any field may be null/empty; the generator degrades gracefully.
     */
    record ReportCardContext(
            Long tenantId,
            String studentName,
            String studentNumber,
            String className,
            String periodLabel,
            String schoolYear,
            Map<Long, String> subjectNames,
            /** Détail mensuel "cours" par matière (§colonnes par mois, secondaire uniquement) — vide
             * en primaire, voir {@link SubjectMonthlyBreakdown}. */
            List<SubjectMonthlyBreakdown> monthlyBreakdown) {

        public static ReportCardContext empty() {
            return new ReportCardContext(null, null, null, null, null, null, Map.of(), List.of());
        }

        public String subjectName(Long subjectId) {
            if (subjectId == null) return "—";
            String name = subjectNames == null ? null : subjectNames.get(subjectId);
            return name != null && !name.isBlank() ? name : "Matière " + subjectId;
        }
    }
}
