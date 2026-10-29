package gn.lacaeducation.grading.application.port.out;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Centralisateur de classe — reproduit fidèlement la maquette de référence officielle (§retour
 * utilisateur, {@code features/design-pro-officiel-centralisateur.pdf}) : "FICHE DE CENTRALISATION DES
 * MOYENNES PAR MATIÈRE", une matière par colonne (coefficient en en-tête de bloc), un groupe de lignes
 * par élève (coef / 1er semestre [cours, comp, Moy 1] / 2ème semestre [cours, comp, Moy 2] / Annuelle),
 * moyenne générale et rang affichés une seule fois sur la ligne "Annuelle" — jamais une moyenne
 * mensuelle (abandonné, voir historique). Même agrégation que le bulletin annuel
 * ({@code AnnualReportCardUseCase}), jamais recalculée différemment pour éviter tout écart entre les
 * deux documents.
 */
public interface ClassCentralizerPdfGenerator {

    byte[] generate(Centralizer data);

    /** Une matière enseignée dans la classe, colonne du tableau — {@code subjectId} sert de clé commune
     * avec les maps de {@link StudentBlock}. */
    record SubjectColumn(Long subjectId, String subjectName, BigDecimal coefficient) {
    }

    /** Un élève, en 8 lignes du tableau (coef / 1er S. cours-comp-Moy1 / 2ème S. cours-comp-Moy2 /
     * Annuelle) — chaque map est indexée par {@code subjectId} (voir {@link SubjectColumn}), absente de
     * la map si la matière n'a aucune note pour ce semestre/élève. */
    record StudentBlock(
            Long studentId, String studentNumber, String studentName, String levelLabel,
            Map<Long, BigDecimal> s1Cours, Map<Long, BigDecimal> s1Compo, Map<Long, BigDecimal> s1Average,
            Map<Long, BigDecimal> s2Cours, Map<Long, BigDecimal> s2Compo, Map<Long, BigDecimal> s2Average,
            Map<Long, BigDecimal> annualBySubject,
            BigDecimal overallAverage, Integer rank) {
    }

    record Centralizer(
            Long tenantId,
            String className,
            String levelLabel,
            String schoolYear,
            List<SubjectColumn> subjects,
            List<StudentBlock> rows) {

        public int studentCount() {
            return rows == null ? 0 : rows.size();
        }
    }
}
