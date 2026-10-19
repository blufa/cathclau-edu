package gn.lacaeducation.grading.adapters.out.pdf;

import gn.lacaeducation.grading.application.port.out.AnnualReportCardPdfGenerator.AnnualReportCardContext;
import gn.lacaeducation.grading.application.port.out.ReportCardPdfGenerator.ReportCardContext;
import gn.lacaeducation.grading.domain.AnnualReportCard;
import gn.lacaeducation.grading.domain.AnnualReportCardLine;
import gn.lacaeducation.grading.domain.Exam;
import gn.lacaeducation.grading.domain.ExamStatus;
import gn.lacaeducation.grading.domain.ExamType;
import gn.lacaeducation.grading.domain.ReportCard;
import gn.lacaeducation.grading.domain.ReportCardLine;
import gn.lacaeducation.grading.domain.ReportCardStatus;
import gn.lacaeducation.grading.domain.SubjectMonthlyBreakdown;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;

class OpenPdfGeneratorsTest {

    // Tenant lookup returns empty → the branded header is skipped (the generator degrades gracefully).
    private final OpenPdfReportCardGenerator reportCardGenerator = new OpenPdfReportCardGenerator(
            new gn.lacaeducation.common.TenantInfoProvider() {
                @Override public Optional<gn.lacaeducation.common.TenantPublicInfo> findTenantInfo(Long tenantId) { return Optional.empty(); }
                @Override public boolean isTenantActive(Long tenantId) { return true; }
            });
    private final OpenPdfExamConvocationGenerator convocationGenerator = new OpenPdfExamConvocationGenerator(
            new gn.lacaeducation.common.TenantInfoProvider() {
                @Override public Optional<gn.lacaeducation.common.TenantPublicInfo> findTenantInfo(Long tenantId) { return Optional.empty(); }
                @Override public boolean isTenantActive(Long tenantId) { return true; }
            });
    private final OpenPdfAnnualReportCardGenerator annualGenerator = new OpenPdfAnnualReportCardGenerator(
            new gn.lacaeducation.common.TenantInfoProvider() {
                @Override public Optional<gn.lacaeducation.common.TenantPublicInfo> findTenantInfo(Long tenantId) { return Optional.empty(); }
                @Override public boolean isTenantActive(Long tenantId) { return true; }
            });

    @Test
    void reportCard_producesValidPdfBytes() {
        ReportCard rc = ReportCard.builder()
                .id(1L).studentId(7L).classId(3L).periodId(2L)
                .overallAverage(new BigDecimal("14.50")).classAverage(new BigDecimal("12.30"))
                .highestAverage(new BigDecimal("17.00")).lowestAverage(new BigDecimal("8.00"))
                .rank(2).classSize(30).honor("BIEN").decision("ADMIS").comment("Bon trimestre")
                .status(ReportCardStatus.GENERATED)
                .build();
        List<ReportCardLine> lines = List.of(
                ReportCardLine.builder().subjectId(11L).subjectAverage(new BigDecimal("15.00"))
                        .coefficient(new BigDecimal("4")).weightedAverage(new BigDecimal("60.00"))
                        .subjectRank(1).teacherName("M. Diallo").build(),
                ReportCardLine.builder().subjectId(12L).subjectAverage(new BigDecimal("13.00"))
                        .coefficient(new BigDecimal("2")).weightedAverage(new BigDecimal("26.00"))
                        .subjectRank(5).build());

        ReportCardContext ctx = new ReportCardContext(1L, "Awa Diallo", "2026-0007", "6e A", "COLLEGE",
                "1er trimestre", "2025-2026", Map.of(11L, "Mathématiques", 12L, "Français"), List.of(),
                null, null, null, null);
        byte[] pdf = reportCardGenerator.generate(rc, lines, ctx);

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
    }

    @Test
    void reportCard_handlesNullsAndEmptyLines() {
        ReportCard rc = ReportCard.builder().id(2L).build();

        byte[] pdf = reportCardGenerator.generate(rc, List.of(), ReportCardContext.empty());

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
    }

    /** Secondaire avec détail mensuel (§colonnes par mois, "bulletin-test" de référence) — plusieurs
     * mois présents, une matière sans note un mois donné (cellule "—" attendue, pas de plantage). */
    @Test
    void reportCard_secondaire_withMonthlyBreakdown_producesValidPdfBytes() {
        ReportCard rc = ReportCard.builder()
                .id(3L).studentId(7L).classId(3L).periodId(2L)
                .overallAverage(new BigDecimal("14.17")).classAverage(new BigDecimal("12.30"))
                .rank(2).classSize(15).honor("ASSEZ BIEN").decision("ADMIS")
                .status(ReportCardStatus.GENERATED)
                .build();
        List<ReportCardLine> lines = List.of(
                ReportCardLine.builder().subjectId(11L).subjectAverage(new BigDecimal("15.00"))
                        .coefficient(new BigDecimal("2")).weightedAverage(new BigDecimal("30.00"))
                        .compoAverage(new BigDecimal("16.50")).subjectRank(1).build(),
                ReportCardLine.builder().subjectId(12L).subjectAverage(new BigDecimal("13.00"))
                        .coefficient(new BigDecimal("1")).weightedAverage(new BigDecimal("13.00"))
                        .compoAverage(new BigDecimal("12.00")).subjectRank(5).build());
        List<SubjectMonthlyBreakdown> monthly = List.of(
                new SubjectMonthlyBreakdown(11L, new BigDecimal("2"),
                        monthlyMap(YearMonth.of(2026, 10), "16.50", YearMonth.of(2026, 11), "14.00"),
                        new BigDecimal("16.50"), new BigDecimal("15.00")),
                // Pas de note en novembre pour cette matière → colonne "—" attendue, pas d'exception.
                new SubjectMonthlyBreakdown(12L, BigDecimal.ONE,
                        monthlyMap(YearMonth.of(2026, 10), "14.00"),
                        new BigDecimal("12.00"), new BigDecimal("13.00")));

        ReportCardContext ctx = new ReportCardContext(1L, "Awa Diallo", "2026-0007", "8e A", "COLLEGE",
                "1er semestre", "2025-2026", Map.of(11L, "Mathématiques", 12L, "Anglais"), monthly,
                null, null, null, null);
        byte[] pdf = reportCardGenerator.generate(rc, lines, ctx);

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
    }

    private static java.util.SortedMap<YearMonth, BigDecimal> monthlyMap(Object... pairs) {
        java.util.SortedMap<YearMonth, BigDecimal> map = new TreeMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put((YearMonth) pairs[i], new BigDecimal((String) pairs[i + 1]));
        }
        return map;
    }

    /** Primaire : tableau simplifié (pas de mois ni de coefficient, voir SubjectSemesterAverage) —
     * monthlyBreakdown vide, coursAverage/compoAverage nuls sur les lignes (seul subjectAverage compte). */
    @Test
    void reportCard_primaire_simplifiedTable_producesValidPdfBytes() {
        ReportCard rc = ReportCard.builder()
                .id(4L).studentId(7L).classId(3L).periodId(9L)
                .overallAverage(new BigDecimal("13.75")).classAverage(new BigDecimal("12.92"))
                .rank(2).classSize(3).honor("PASSABLE").decision("ADMIS")
                .status(ReportCardStatus.GENERATED)
                .build();
        List<ReportCardLine> lines = List.of(
                ReportCardLine.builder().subjectId(21L).subjectAverage(new BigDecimal("18.00"))
                        .coefficient(BigDecimal.ONE).weightedAverage(new BigDecimal("18.00")).subjectRank(1).build());

        ReportCardContext ctx = new ReportCardContext(1L, "Alassane Diallo", "PNM00079", "CM1", "PRIMAIRE",
                "1er trimestre", "2026-2027", Map.of(21L, "Français (Dictée - Questions)"), List.of(),
                null, null, null, null);
        byte[] pdf = reportCardGenerator.generate(rc, lines, ctx);

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
    }

    /** Bulletin de fin d'année : 2ème semestre en colonnes mensuelles + bloc BILAN (absences/retards). */
    @Test
    void annual_withMonthlyBreakdownAndAttendance_producesValidPdfBytes() {
        AnnualReportCard card = AnnualReportCard.builder()
                .studentId(7L).classId(3L).academicYearId(5L).s1PeriodId(12L).s2PeriodId(14L)
                .s1Average(new BigDecimal("12.23")).s2Average(new BigDecimal("12.46"))
                .overallAverage(new BigDecimal("12.35")).rank(12).classSize(64)
                .honor("ASSEZ BIEN").decision("ADMIS").absentCount(3).lateCount(5)
                .lines(List.of(AnnualReportCardLine.builder().subjectId(11L).coefficient(new BigDecimal("2"))
                        .s1Average(new BigDecimal("13.00")).s2Compo(new BigDecimal("14.00"))
                        .s2Average(new BigDecimal("14.50")).annualAverage(new BigDecimal("13.75")).build()))
                .build();
        List<SubjectMonthlyBreakdown> s2Monthly = List.of(new SubjectMonthlyBreakdown(11L, new BigDecimal("2"),
                monthlyMap(YearMonth.of(2027, 2), "16.50", YearMonth.of(2027, 3), "15.00"),
                new BigDecimal("14.00"), new BigDecimal("14.50")));

        AnnualReportCardContext ctx = new AnnualReportCardContext(1L, "Mariama Korka Diakhaby", "2400241",
                "7e", "COLLEGE", "1er Semestre", "2ème Semestre", "2026-2027", Map.of(11L, "Mathématiques"), s2Monthly);
        byte[] pdf = annualGenerator.generate(card, ctx);

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
    }

    @Test
    void convocation_producesValidPdfBytes() {
        Exam exam = Exam.builder()
                .id(1L).tenantId(1L).classId(3L).periodId(2L)
                .type(ExamType.SEMESTER).label("Composition de fin de semestre")
                .scheduledDate(LocalDate.of(2026, 6, 20)).status(ExamStatus.PLANNED)
                .build();

        byte[] pdf = convocationGenerator.generate(exam,
                new gn.lacaeducation.grading.application.port.out.ExamConvocationPdfGenerator.ExamConvocationContext(
                        "Terminale S", "Semestre 2", "2025-2026"));

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
    }
}
