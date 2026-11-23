package gn.lacaeducation.grading.application.service;

import gn.lacaeducation.academics.AcademicsDataProvider;
import gn.lacaeducation.grading.TeacherNameProvider;
import gn.lacaeducation.grading.application.port.out.AssessmentRepository;
import gn.lacaeducation.grading.application.port.out.ClassSubjectQueryPort;
import gn.lacaeducation.grading.application.port.out.ClassSubjectQueryPort.ClassSubjectInfo;
import gn.lacaeducation.grading.application.port.out.GradeRepository;
import gn.lacaeducation.academics.ClassPublicInfo;
import gn.lacaeducation.grading.application.port.out.ClassCentralizerPdfGenerator.SubjectColumn;
import gn.lacaeducation.grading.application.port.out.MonthlyClassCentralizerPdfGenerator;
import gn.lacaeducation.grading.application.port.out.MonthlyClassCentralizerPdfGenerator.MonthlyCentralizer;
import gn.lacaeducation.grading.application.port.out.MonthlyReportCardPdfGenerator;
import gn.lacaeducation.grading.application.port.out.MonthlyReportCardRepository;
import gn.lacaeducation.grading.domain.*;
import gn.lacaeducation.students.StudentDataProvider;
import gn.lacaeducation.students.StudentPublicInfo;
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
    @Mock MonthlyClassCentralizerPdfGenerator centralizerGenerator;
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

    @Test
    void classCentralizerPdf_buildsSubjectColumnsAndRowsSortedByRank_noNewComputation() {
        Long classId = 1L;
        MonthlyReportCard first = MonthlyReportCard.builder().id(50L).tenantId(9L).studentId(10L).classId(classId)
                .yearMonth(MONTH).overallAverage(BigDecimal.valueOf(16)).rank(1).build();
        MonthlyReportCard second = MonthlyReportCard.builder().id(51L).tenantId(9L).studentId(20L).classId(classId)
                .yearMonth(MONTH).overallAverage(BigDecimal.valueOf(12)).rank(2).build();
        // Volontairement renvoyés dans le désordre par le repository — le centralisateur doit les
        // retrier par rang, jamais faire confiance à l'ordre du repository.
        when(monthlyReportCardRepository.findByClassIdAndYearMonth(classId, MONTH)).thenReturn(List.of(second, first));
        when(classSubjectQueryPort.findByClassId(classId))
                .thenReturn(List.of(new ClassSubjectInfo(100L, BigDecimal.valueOf(3), 5L)));
        when(academicsDataProvider.subjectLabels(any())).thenReturn(java.util.Map.of(100L, "Mathématiques"));
        when(academicsDataProvider.findClass(classId))
                .thenReturn(Optional.of(new ClassPublicInfo(classId, "6ème A", "Collège", "NORMALE", null, null, "COLLEGE")));
        when(academicsDataProvider.currentAcademicYearLabel(9L)).thenReturn(Optional.of("2026-2027"));
        when(studentDataProvider.findStudent(10L)).thenReturn(Optional.of(
                new StudentPublicInfo(10L, 9L, "Awa", "Bah", null, null, "MAT-1", null, null, null, null)));
        when(studentDataProvider.findStudent(20L)).thenReturn(Optional.of(
                new StudentPublicInfo(20L, 9L, "Moussa", "Camara", null, null, "MAT-2", null, null, null, null)));
        when(monthlyReportCardRepository.findLines(50L)).thenReturn(List.of(
                MonthlyReportCardLine.builder().monthlyReportCardId(50L).subjectId(100L).subjectAverage(BigDecimal.valueOf(16)).build()));
        when(monthlyReportCardRepository.findLines(51L)).thenReturn(List.of(
                MonthlyReportCardLine.builder().monthlyReportCardId(51L).subjectId(100L).subjectAverage(BigDecimal.valueOf(12)).build()));
        when(centralizerGenerator.generate(any())).thenReturn(new byte[]{1, 2, 3});

        byte[] pdf = service.classCentralizerPdf(classId, MONTH);

        assertThat(pdf).isEqualTo(new byte[]{1, 2, 3});
        ArgumentCaptor<MonthlyCentralizer> captor = ArgumentCaptor.forClass(MonthlyCentralizer.class);
        verify(centralizerGenerator).generate(captor.capture());
        MonthlyCentralizer data = captor.getValue();

        assertThat(data.tenantId()).isEqualTo(9L);
        assertThat(data.className()).isEqualTo("6ème A (Collège)");
        assertThat(data.levelLabel()).isEqualTo("Collège");
        assertThat(data.schoolYear()).isEqualTo("2026-2027");
        assertThat(data.monthLabel()).isEqualTo("Novembre 2026");
        assertThat(data.subjects()).extracting(SubjectColumn::subjectName).containsExactly("Mathématiques");
        assertThat(data.subjects().get(0).coefficient()).isEqualByComparingTo("3");

        // Jamais de nouveau calcul de moyenne/rang : les valeurs des MonthlyReportCard existants sont
        // reprises telles quelles, et l'ordre suit le rang (1 avant 2), pas l'ordre du repository.
        assertThat(data.rows()).extracting(gn.lacaeducation.grading.application.port.out.MonthlyClassCentralizerPdfGenerator.MonthlyStudentBlock::studentId)
                .containsExactly(10L, 20L);
        assertThat(data.rows().get(0).studentName()).isEqualTo("BAH Awa");
        assertThat(data.rows().get(0).overallAverage()).isEqualByComparingTo("16");
        assertThat(data.rows().get(0).rank()).isEqualTo(1);
        assertThat(data.rows().get(0).averageBySubject().get(100L)).isEqualByComparingTo("16");
        verify(monthlyReportCardRepository, never()).save(any());
    }

    @Test
    void classCentralizerPdf_studentWithoutLine_hasEmptyAverageMap() {
        Long classId = 1L;
        MonthlyReportCard card = MonthlyReportCard.builder().id(60L).tenantId(9L).studentId(30L).classId(classId)
                .yearMonth(MONTH).overallAverage(null).rank(null).build();
        when(monthlyReportCardRepository.findByClassIdAndYearMonth(classId, MONTH)).thenReturn(List.of(card));
        when(classSubjectQueryPort.findByClassId(classId)).thenReturn(List.of());
        when(academicsDataProvider.subjectLabels(any())).thenReturn(java.util.Map.of());
        when(academicsDataProvider.findClass(classId)).thenReturn(Optional.empty());
        when(studentDataProvider.findStudent(30L)).thenReturn(Optional.empty());
        when(monthlyReportCardRepository.findLines(60L)).thenReturn(List.of());
        when(centralizerGenerator.generate(any())).thenReturn(new byte[0]);

        service.classCentralizerPdf(classId, MONTH);

        ArgumentCaptor<MonthlyCentralizer> captor = ArgumentCaptor.forClass(MonthlyCentralizer.class);
        verify(centralizerGenerator).generate(captor.capture());
        MonthlyCentralizer data = captor.getValue();
        assertThat(data.rows()).hasSize(1);
        assertThat(data.rows().get(0).studentName()).isEqualTo("Élève #30");
        assertThat(data.rows().get(0).averageBySubject()).isEmpty();
    }
}
