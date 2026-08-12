package gn.lacaeducation.grading.application.port.out;

import gn.lacaeducation.grading.domain.AnnualReportCard;

import java.util.Map;

/** Génère le PDF du bulletin annuel officiel (mise en page ministère : COURS/COMPO/MOY par semestre). */
public interface AnnualReportCardPdfGenerator {

    byte[] generate(AnnualReportCard card, AnnualReportCardContext context);

    /** Noms résolus depuis les autres modules — jamais d'identifiant brut affiché. */
    record AnnualReportCardContext(
            Long tenantId,
            String studentName,
            String studentNumber,
            String className,
            String s1Label,
            String s2Label,
            String schoolYear,
            Map<Long, String> subjectNames) {

        public String subjectName(Long subjectId) {
            if (subjectId == null) return "—";
            String name = subjectNames == null ? null : subjectNames.get(subjectId);
            return name != null && !name.isBlank() ? name : "Matière " + subjectId;
        }
    }
}
