package gn.lacaeducation.grading.adapters.out;

import gn.lacaeducation.grading.SchoolAcademicStatsProvider.AcademicStats;
import gn.lacaeducation.grading.application.port.out.ReportCardRepository;
import gn.lacaeducation.grading.domain.ReportCard;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchoolAcademicStatsProviderAdapterTest {

    @Mock ReportCardRepository reportCardRepository;
    @InjectMocks SchoolAcademicStatsProviderAdapter adapter;

    private ReportCard rc(Long studentId, Long classId, String avg, Integer rank, String decision) {
        return ReportCard.builder().studentId(studentId).classId(classId)
                .overallAverage(new BigDecimal(avg)).rank(rank).decision(decision).build();
    }

    @Test
    void academicStats_computesPassRateAveragesAndRanking() {
        when(reportCardRepository.findByPeriodId(2L)).thenReturn(List.of(
                rc(10L, 100L, "14", 1, "ADMIS"),
                rc(11L, 100L, "8", 2, "REDOUBLE"),
                rc(12L, 200L, "16", 1, "ADMIS")));

        AcademicStats s = adapter.academicStats(2L, 10);

        assertThat(s.studentCount()).isEqualTo(3);
        // (14+8+16)/3 = 12.67
        assertThat(s.overallAverage()).isEqualByComparingTo("12.67");
        // 2 ADMIS / 3 = 66.67 %
        assertThat(s.passRate()).isEqualByComparingTo("66.67");
        assertThat(s.classStats()).hasSize(2);
        // Classement global : 16 puis 14 puis 8
        assertThat(s.topStudents()).extracting(r -> r.overallAverage().stripTrailingZeros().toPlainString())
                .containsExactly("16", "14", "8");
        // Classe 100 : moyenne (14+8)/2 = 11.00, taux 1/2 = 50 %
        var class100 = s.classStats().stream().filter(c -> c.classId().equals(100L)).findFirst().orElseThrow();
        assertThat(class100.classAverage()).isEqualByComparingTo("11.00");
        assertThat(class100.passRate()).isEqualByComparingTo("50.00");
        assertThat(class100.highestAverage()).isEqualByComparingTo("14");
        assertThat(class100.lowestAverage()).isEqualByComparingTo("8");
    }

    @Test
    void academicStats_emptyPeriod_isZero() {
        when(reportCardRepository.findByPeriodId(2L)).thenReturn(List.of());
        AcademicStats s = adapter.academicStats(2L, 10);
        assertThat(s.studentCount()).isZero();
        assertThat(s.passRate()).isEqualByComparingTo("0");
        assertThat(s.classStats()).isEmpty();
        assertThat(s.topStudents()).isEmpty();
    }
}
