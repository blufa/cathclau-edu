package gn.lacaeducation.grading.adapters.out.pdf;

import gn.lacaeducation.grading.domain.Exam;
import gn.lacaeducation.grading.domain.ExamStatus;
import gn.lacaeducation.grading.domain.ExamType;
import gn.lacaeducation.grading.domain.ReportCard;
import gn.lacaeducation.grading.domain.ReportCardLine;
import gn.lacaeducation.grading.domain.ReportCardStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OpenPdfGeneratorsTest {

    private final OpenPdfReportCardGenerator reportCardGenerator = new OpenPdfReportCardGenerator();
    private final OpenPdfExamConvocationGenerator convocationGenerator = new OpenPdfExamConvocationGenerator();

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

        byte[] pdf = reportCardGenerator.generate(rc, lines);

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
    }

    @Test
    void reportCard_handlesNullsAndEmptyLines() {
        ReportCard rc = ReportCard.builder().id(2L).build();

        byte[] pdf = reportCardGenerator.generate(rc, List.of());

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

        byte[] pdf = convocationGenerator.generate(exam);

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
    }
}
