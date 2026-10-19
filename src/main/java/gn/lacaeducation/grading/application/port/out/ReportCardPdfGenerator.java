package gn.lacaeducation.grading.application.port.out;

import gn.lacaeducation.grading.domain.ReportCard;
import gn.lacaeducation.grading.domain.ReportCardLine;
import gn.lacaeducation.grading.domain.SubjectMonthlyBreakdown;

import java.math.BigDecimal;
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
            /** Cycle de la classe ("COLLEGE"/"LYCEE"/...) — ligne "OPTION / NIVEAU" du bulletin
             * (§reproduction fidèle bulletin-test.webp), distincte du libellé de classe lui-même. */
            String cycleLabel,
            String periodLabel,
            String schoolYear,
            Map<Long, String> subjectNames,
            /** Détail mensuel "cours" par matière (§colonnes par mois, secondaire uniquement) — vide
             * en primaire, voir {@link SubjectMonthlyBreakdown}. */
            List<SubjectMonthlyBreakdown> monthlyBreakdown,
            /** Semestre précédent de la même année scolaire (ex. "1er Semestre" affiché sur le bulletin
             * du 2ème) — null si ce bulletin est le premier de l'année, ou si le bulletin du semestre
             * précédent n'a pas encore été généré (§retour fondateur). */
            String previousSemesterLabel, BigDecimal previousSemesterAverage, String previousSemesterHonor,
            /** Photo de l'élève (URL) — carte d'identité du bulletin (§retour fondateur : "je ne vois pas
             * la photo de l'élève"), même source que celle utilisée sur sa fiche/son badge. */
            String studentPhoto) {

        public static ReportCardContext empty() {
            return new ReportCardContext(null, null, null, null, null, null, null, Map.of(), List.of(), null, null, null, null);
        }

        public String subjectName(Long subjectId) {
            if (subjectId == null) return "—";
            String name = subjectNames == null ? null : subjectNames.get(subjectId);
            return name != null && !name.isBlank() ? name : "Matière " + subjectId;
        }
    }
}
