package gn.lacaeducation.grading.application.service;

import gn.lacaeducation.grading.application.port.out.AssessmentRepository;
import gn.lacaeducation.grading.application.port.out.ClassSubjectQueryPort;
import gn.lacaeducation.grading.application.port.out.ClassSubjectQueryPort.ClassSubjectInfo;
import gn.lacaeducation.grading.application.port.out.GradeRepository;
import gn.lacaeducation.grading.application.port.out.ReportCardRepository;
import gn.lacaeducation.grading.domain.Assessment;
import gn.lacaeducation.grading.domain.Grade;
import gn.lacaeducation.grading.domain.ReportCard;
import gn.lacaeducation.grading.domain.ReportCardLine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GradeCalculationServiceTest {

    @Mock AssessmentRepository assessmentRepository;
    @Mock GradeRepository gradeRepository;
    @Mock ReportCardRepository reportCardRepository;
    @Mock ClassSubjectQueryPort classSubjectQueryPort;
    @Mock ApplicationEventPublisher eventPublisher;
    @InjectMocks GradeCalculationService service;

    @Test
    void honorAndDecision_thresholds() {
        assertThat(GradeCalculationService.computeHonor(BigDecimal.valueOf(18))).isEqualTo("Très Bien");
        assertThat(GradeCalculationService.computeHonor(BigDecimal.valueOf(16))).isEqualTo("Bien");
        assertThat(GradeCalculationService.computeHonor(BigDecimal.valueOf(14))).isEqualTo("Assez Bien");
        assertThat(GradeCalculationService.computeHonor(BigDecimal.valueOf(10))).isEqualTo("Passable");
        assertThat(GradeCalculationService.computeHonor(BigDecimal.valueOf(8))).isEqualTo("Insuffisant");
        assertThat(GradeCalculationService.computeDecision(BigDecimal.valueOf(10))).isEqualTo("ADMIS");
        assertThat(GradeCalculationService.computeDecision(BigDecimal.valueOf(9))).isEqualTo("REDOUBLE");
    }

    @Test
    void generateForStudent_computesWeightedAverages() {
        Long classId = 1L, periodId = 2L, studentId = 10L, subjectId = 100L;
        Assessment a1 = Assessment.builder().id(1L).classId(classId).periodId(periodId).subjectId(subjectId)
                .coefficient(BigDecimal.valueOf(2)).maxScore(BigDecimal.valueOf(20)).build();
        Assessment a2 = Assessment.builder().id(2L).classId(classId).periodId(periodId).subjectId(subjectId)
                .coefficient(BigDecimal.valueOf(1)).maxScore(BigDecimal.valueOf(20)).build();
        when(assessmentRepository.findByClassIdAndPeriodId(classId, periodId)).thenReturn(List.of(a1, a2));
        when(classSubjectQueryPort.findByClassId(classId))
                .thenReturn(List.of(new ClassSubjectInfo(subjectId, BigDecimal.valueOf(3), 5L)));
        when(gradeRepository.findByAssessmentId(1L))
                .thenReturn(List.of(Grade.builder().assessmentId(1L).studentId(studentId).value(BigDecimal.valueOf(12)).build()));
        when(gradeRepository.findByAssessmentId(2L))
                .thenReturn(List.of(Grade.builder().assessmentId(2L).studentId(studentId).value(BigDecimal.valueOf(15)).build()));
        when(reportCardRepository.findByStudentIdAndPeriodId(studentId, periodId)).thenReturn(Optional.empty());
        when(reportCardRepository.save(any())).thenAnswer(i -> { ReportCard r = i.getArgument(0); if (r.getId() == null) r.setId(99L); return r; });
        when(reportCardRepository.saveLine(any())).thenAnswer(i -> i.getArgument(0));
        when(reportCardRepository.findByClassIdAndPeriodId(classId, periodId)).thenReturn(List.of());
        when(reportCardRepository.findById(any())).thenAnswer(i -> Optional.of(ReportCard.builder().id(99L).build()));

        service.generateForStudent(studentId, classId, periodId);

        ArgumentCaptor<ReportCard> rcCaptor = ArgumentCaptor.forClass(ReportCard.class);
        ArgumentCaptor<ReportCardLine> lineCaptor = ArgumentCaptor.forClass(ReportCardLine.class);
        org.mockito.Mockito.verify(reportCardRepository, org.mockito.Mockito.atLeastOnce()).save(rcCaptor.capture());
        org.mockito.Mockito.verify(reportCardRepository, org.mockito.Mockito.atLeastOnce()).saveLine(lineCaptor.capture());

        // subject average = (12*2 + 15*1) / 3 = 13.00 ; weighted = 13 * 3 = 39.00 ; general = 39/3 = 13.00
        ReportCard rc = rcCaptor.getAllValues().get(0);
        assertThat(rc.getOverallAverage()).isEqualByComparingTo("13.00");
        assertThat(rc.getHonor()).isEqualTo("Passable");
        assertThat(rc.getDecision()).isEqualTo("ADMIS");

        ReportCardLine line = lineCaptor.getAllValues().get(0);
        assertThat(line.getSubjectAverage()).isEqualByComparingTo("13.00");
        assertThat(line.getWeightedAverage()).isEqualByComparingTo("39.00");
    }
}
