package gn.lacaeducation.grading.application.service;

import gn.lacaeducation.academics.AcademicsDataProvider;
import gn.lacaeducation.grading.TeacherNameProvider;
import gn.lacaeducation.grading.application.port.out.AssessmentRepository;
import gn.lacaeducation.grading.application.port.out.ClassSubjectQueryPort;
import gn.lacaeducation.grading.application.port.out.ClassSubjectQueryPort.ClassSubjectInfo;
import gn.lacaeducation.grading.application.port.out.GradeRepository;
import gn.lacaeducation.grading.application.port.out.MonthlyReportCardPdfGenerator;
import gn.lacaeducation.grading.application.port.out.MonthlyReportCardRepository;
import gn.lacaeducation.grading.domain.*;
import gn.lacaeducation.students.StudentDataProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * §censeur : "évaluation mensuelle" — diagnostic de niveau en cours d'année, saisi dans la période
 * semestrielle en cours (pas une nouvelle période) mais généré dans un sous-domaine totalement séparé
 * du bulletin officiel (jamais la même table), avec appréciation automatique ("pour que son parent
 * puisse savoir si il doit faire des révisions") et appréciation manuelle du censeur conservée d'une
 * régénération à l'autre.
 */
@ExtendWith(MockitoExtension.class)
class MonthlyReportCardServiceTest {

    @Mock AssessmentRepository assessmentRepository;
    @Mock GradeRepository gradeRepository;
    @Mock MonthlyReportCardRepository monthlyReportCardRepository;
    @Mock ClassSubjectQueryPort classSubjectQueryPort;
    @Mock AcademicsDataProvider academicsDataProvider;
    @Mock TeacherNameProvider teacherNameProvider;
    @Mock StudentDataProvider studentDataProvider;
    @Mock MonthlyReportCardPdfGenerator pdfGenerator;
    @InjectMocks MonthlyReportCardService service;

    private static final YearMonth MONTH = YearMonth.of(2026, 11);

    private Assessment monthlyAssessment(Long id, Long classId, Long subjectId, LocalDate date) {
        return Assessment.builder().id(id).classId(classId).subjectId(subjectId)
                .type(AssessmentType.MONTHLY).date(date).maxScore(BigDecimal.valueOf(20)).build();
    }

    @Test
    void generateForStudent_secondaire_scalesByCoefficient_andSetsAutoAppreciation() {
        Long classId = 1L, studentId = 10L, subjectId = 100L;
        Assessment a = monthlyAssessment(1L, classId, subjectId, LocalDate.of(2026, 11, 12));
        when(assessmentRepository.findByClassId(classId)).thenReturn(List.of(a));
        when(classSubjectQueryPort.findByClassId(classId))
                .thenReturn(List.of(new ClassSubjectInfo(subjectId, BigDecimal.valueOf(2), 5L)));
        when(gradeRepository.findByAssessmentId(1L))
                .thenReturn(List.of(Grade.builder().assessmentId(1L).studentId(studentId).value(BigDecimal.valueOf(18)).build()));
        when(monthlyReportCardRepository.findByStudentIdAndClassIdAndYearMonth(studentId, classId, MONTH)).thenReturn(Optional.empty());
        when(monthlyReportCardRepository.save(any())).thenAnswer(i -> { MonthlyReportCard c = i.getArgument(0); if (c.getId() == null) c.setId(50L); return c; });
        when(monthlyReportCardRepository.saveLine(any())).thenAnswer(i -> i.getArgument(0));

        MonthlyReportCard result = service.generateForStudent(studentId, classId, MONTH);

        // 2 × 18 / 1 = 36.00, overall = 36 / 2 (coef) = 18.00
        assertThat(result.getOverallAverage()).isEqualByComparingTo("18.00");
        assertThat(result.getAutoAppreciation()).contains("Excellent");
        assertThat(result.getYearMonth()).isEqualTo(MONTH);

        ArgumentCaptor<MonthlyReportCardLine> lineCaptor = ArgumentCaptor.forClass(MonthlyReportCardLine.class);
        verify(monthlyReportCardRepository).saveLine(lineCaptor.capture());
        assertThat(lineCaptor.getValue().getSubjectAverage()).isEqualByComparingTo("36.00");
    }

    @Test
    void generateForStudent_lowAverage_appreciationRecommendsRevision() {
        Long classId = 1L, studentId = 10L, subjectId = 100L;
        Assessment a = monthlyAssessment(1L, classId, subjectId, LocalDate.of(2026, 11, 12));
        when(assessmentRepository.findByClassId(classId)).thenReturn(List.of(a));
        when(classSubjectQueryPort.findByClassId(classId))
                .thenReturn(List.of(new ClassSubjectInfo(subjectId, BigDecimal.ONE, 5L)));
        when(gradeRepository.findByAssessmentId(1L))
                .thenReturn(List.of(Grade.builder().assessmentId(1L).studentId(studentId).value(BigDecimal.valueOf(6)).build()));
        when(monthlyReportCardRepository.findByStudentIdAndClassIdAndYearMonth(studentId, classId, MONTH)).thenReturn(Optional.empty());
        when(monthlyReportCardRepository.save(any())).thenAnswer(i -> { MonthlyReportCard c = i.getArgument(0); if (c.getId() == null) c.setId(51L); return c; });
        when(monthlyReportCardRepository.saveLine(any())).thenAnswer(i -> i.getArgument(0));

        MonthlyReportCard result = service.generateForStudent(studentId, classId, MONTH);

        assertThat(result.getOverallAverage()).isEqualByComparingTo("6.00");
        assertThat(result.getAutoAppreciation()).contains("révisions sont nécessaires");
    }

    @Test
    void regenerating_preservesExistingManualComment() {
        Long classId = 1L, studentId = 10L, subjectId = 100L;
        Assessment a = monthlyAssessment(1L, classId, subjectId, LocalDate.of(2026, 11, 12));
        when(assessmentRepository.findByClassId(classId)).thenReturn(List.of(a));
        when(classSubjectQueryPort.findByClassId(classId))
                .thenReturn(List.of(new ClassSubjectInfo(subjectId, BigDecimal.ONE, 5L)));
        when(gradeRepository.findByAssessmentId(1L))
                .thenReturn(List.of(Grade.builder().assessmentId(1L).studentId(studentId).value(BigDecimal.valueOf(15)).build()));
        MonthlyReportCard existing = MonthlyReportCard.builder().id(42L).studentId(studentId).classId(classId)
                .yearMonth(MONTH).comment("Bon trimestre, continue comme ça").build();
        when(monthlyReportCardRepository.findByStudentIdAndClassIdAndYearMonth(studentId, classId, MONTH)).thenReturn(Optional.of(existing));
        ArgumentCaptor<MonthlyReportCard> cardCaptor = ArgumentCaptor.forClass(MonthlyReportCard.class);
        when(monthlyReportCardRepository.save(cardCaptor.capture())).thenAnswer(i -> { MonthlyReportCard c = i.getArgument(0); if (c.getId() == null) c.setId(52L); return c; });
        when(monthlyReportCardRepository.saveLine(any())).thenAnswer(i -> i.getArgument(0));

        service.generateForStudent(studentId, classId, MONTH);

        // La ligne existante (id=42) est réutilisée (UPDATE), jamais supprimée puis réinsérée — sinon
        // l'ordre de flush par défaut d'Hibernate (INSERT avant DELETE) viole la contrainte unique
        // (student_id, class_id, year_month), voir persistWithLines.
        verify(monthlyReportCardRepository).deleteLinesByMonthlyReportCardId(42L);
        verify(monthlyReportCardRepository, never()).deleteById(any());
        assertThat(cardCaptor.getValue().getId()).isEqualTo(42L);
        assertThat(cardCaptor.getValue().getComment()).isEqualTo("Bon trimestre, continue comme ça");
    }

    @Test
    void setComment_updatesOnlyComment() {
        MonthlyReportCard existing = MonthlyReportCard.builder().id(1L).overallAverage(BigDecimal.TEN).build();
        when(monthlyReportCardRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(monthlyReportCardRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        MonthlyReportCard result = service.setComment(1L, "Peut mieux faire en mathématiques");

        assertThat(result.getComment()).isEqualTo("Peut mieux faire en mathématiques");
        assertThat(result.getOverallAverage()).isEqualByComparingTo("10");
    }

    @Test
    void generateForClass_onlyConsidersStudentsWithAMonthlyGrade() {
        Long classId = 1L, subjectId = 100L;
        Assessment a = monthlyAssessment(1L, classId, subjectId, LocalDate.of(2026, 11, 12));
        when(assessmentRepository.findByClassId(classId)).thenReturn(List.of(a));
        when(gradeRepository.findByAssessmentId(1L)).thenReturn(List.of(
                Grade.builder().assessmentId(1L).studentId(10L).value(BigDecimal.valueOf(12)).build(),
                Grade.builder().assessmentId(1L).studentId(20L).value(BigDecimal.valueOf(14)).build()));
        when(classSubjectQueryPort.findByClassId(classId))
                .thenReturn(List.of(new ClassSubjectInfo(subjectId, BigDecimal.ONE, 5L)));
        when(monthlyReportCardRepository.findByStudentIdAndClassIdAndYearMonth(any(), eq(classId), eq(MONTH))).thenReturn(Optional.empty());
        when(monthlyReportCardRepository.save(any())).thenAnswer(i -> { MonthlyReportCard c = i.getArgument(0); if (c.getId() == null) c.setId((long) (Math.random() * 1000)); return c; });
        when(monthlyReportCardRepository.saveLine(any())).thenAnswer(i -> i.getArgument(0));

        List<MonthlyReportCard> cards = service.generateForClass(classId, MONTH);

        assertThat(cards).hasSize(2);
        assertThat(cards).extracting(MonthlyReportCard::getStudentId).containsExactlyInAnyOrder(10L, 20L);
    }
}
