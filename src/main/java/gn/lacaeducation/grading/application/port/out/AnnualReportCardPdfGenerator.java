package gn.lacaeducation.grading.application.port.out;

import gn.lacaeducation.grading.domain.AnnualReportCard;
import gn.lacaeducation.grading.domain.SubjectMonthlyBreakdown;

import java.util.List;
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
            /** Cycle de la classe ("COLLEGE"/"LYCEE"/...) — ligne "OPTION / NIVEAU" du bulletin
             * (§reproduction fidèle bulletin-test.webp), distincte du libellé de classe lui-même. */
            String cycleLabel,
            String s1Label,
            String s2Label,
            String schoolYear,
            Map<Long, String> subjectNames,
            /** Détail mensuel "cours" par matière du 2ème semestre uniquement (§colonnes par mois, comme
             * le bulletin de fin d'année de référence) — vide en primaire. */
            List<SubjectMonthlyBreakdown> s2MonthlyBreakdown) {

        public String subjectName(Long subjectId) {
            if (subjectId == null) return "—";
            String name = subjectNames == null ? null : subjectNames.get(subjectId);
            return name != null && !name.isBlank() ? name : "Matière " + subjectId;
        }
    }
}
