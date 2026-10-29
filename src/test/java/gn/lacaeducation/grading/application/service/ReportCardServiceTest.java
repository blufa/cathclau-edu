package gn.lacaeducation.grading.application.service;

import gn.lacaeducation.academics.AcademicsDataProvider;
import gn.lacaeducation.academics.ClassPublicInfo;
import gn.lacaeducation.academics.PeriodPublicInfo;
import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.grading.application.port.out.HonorRollPdfGenerator;
import gn.lacaeducation.grading.application.port.out.HonorRollPdfGenerator.HonorRoll;
import gn.lacaeducation.grading.application.port.out.HonorRollPdfGenerator.Row;
import gn.lacaeducation.grading.application.port.out.ReportCardRepository;
import gn.lacaeducation.grading.domain.ReportCard;
import gn.lacaeducation.students.StudentDataProvider;
import gn.lacaeducation.students.StudentPublicInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportCardServiceTest {

    @Mock ReportCardRepository repo;
    @Mock GradeCalculationService calculationService;
    @Mock org.springframework.context.ApplicationEventPublisher eventPublisher;
    @Mock gn.lacaeducation.grading.application.port.out.ReportCardPdfGenerator pdfGenerator;
    @Mock gn.lacaeducation.grading.application.port.out.DocumentStoragePort documentStorage;
    @Mock StudentDataProvider studentProvider;
    @Mock AcademicsDataProvider academicsProvider;
    @Mock gn.lacaeducation.grading.application.port.out.ClassSubjectQueryPort classSubjectQueryPort;
    @Mock HonorRollPdfGenerator honorRollGenerator;
    @Mock gn.lacaeducation.grading.application.port.out.ClassCentralizerPdfGenerator centralizerGenerator;
    @Mock gn.lacaeducation.grading.application.port.in.AnnualReportCardUseCase annualReportCardUseCase;
    @InjectMocks ReportCardService service;

    @AfterEach
    void clearTenant() { TenantContext.clear(); }

    private ReportCard card(Long id, Long studentId, Long classId, BigDecimal average) {
        return ReportCard.builder().id(id).studentId(studentId).classId(classId).periodId(99L)
                .overallAverage(average).honor("—").build();
    }

    private StudentPublicInfo student(Long id, String first, String last) {
        return new StudentPublicInfo(id, 4L, first, last, null, null, null, null, null, null, null);
    }

    /**
     * Le cœur du correctif (§retour fondateur) : le palmarès doit classer les élèves de TOUTE l'école,
     * pas d'une seule classe — un 2e de 8ème A avec 17 doit passer DEVANT le 1er de CM1 avec 14, parce
     * que le classement se fait sur la moyenne générale toutes classes confondues, jamais sur le rang
     * (qui est par construction local à chaque classe).
     */
    @Test
    void honorRollPdf_ranksAcrossClassesByAverage_notByPerClassRank() {
        TenantContext.set(4L);
        // CM1 (classe 49) : 1er de sa classe à 14 ; 8ème A (classe 52) : 2e de sa classe à 17.
        ReportCard cm1First = card(1L, 101L, 49L, BigDecimal.valueOf(14));
        ReportCard eighthSecond = card(2L, 102L, 52L, BigDecimal.valueOf(17));
        ReportCard eighthThird = card(3L, 103L, 52L, BigDecimal.valueOf(16));
        when(repo.findByPeriodId(99L)).thenReturn(List.of(cm1First, eighthSecond, eighthThird));

        when(studentProvider.findStudent(101L)).thenReturn(Optional.of(student(101L, "Alassane", "Diallo")));
        when(studentProvider.findStudent(102L)).thenReturn(Optional.of(student(102L, "Fatoumata", "Bah")));
        when(studentProvider.findStudent(103L)).thenReturn(Optional.of(student(103L, "Mamadou", "Sow")));

        when(academicsProvider.findClass(49L)).thenReturn(Optional.of(
                new ClassPublicInfo(49L, "CM1", "Primaire", null, null, null, null)));
        when(academicsProvider.findClass(52L)).thenReturn(Optional.of(
                new ClassPublicInfo(52L, "8ème A", "Collège", null, null, null, null)));
        when(academicsProvider.findPeriod(99L)).thenReturn(Optional.of(
                new PeriodPublicInfo(99L, "1er Trimestre", "TRIMESTRE", null, null, null, 5L)));
        when(academicsProvider.currentAcademicYearLabel(4L)).thenReturn(Optional.of("2026 - 2027"));
        when(honorRollGenerator.generate(any())).thenReturn(new byte[]{1});

        service.honorRollPdf(99L);

        ArgumentCaptor<HonorRoll> captor = ArgumentCaptor.forClass(HonorRoll.class);
        verify(honorRollGenerator).generate(captor.capture());
        List<Row> rows = captor.getValue().rows();

        assertThat(rows).extracting(Row::studentLastName).containsExactly("Bah", "Sow", "Diallo");
        assertThat(rows).extracting(Row::rank).containsExactly(1, 2, 3);
        assertThat(rows).extracting(Row::className).containsExactly("8ème A (Collège)", "8ème A (Collège)", "CM1 (Primaire)");
    }

    @Test
    void honorRollPdf_cardsWithoutAverage_areExcluded() {
        TenantContext.set(4L);
        ReportCard noAverage = card(1L, 101L, 49L, null);
        when(repo.findByPeriodId(99L)).thenReturn(List.of(noAverage));
        when(academicsProvider.findPeriod(anyLong())).thenReturn(Optional.empty());
        when(honorRollGenerator.generate(any())).thenReturn(new byte[]{1});

        service.honorRollPdf(99L);

        ArgumentCaptor<HonorRoll> captor = ArgumentCaptor.forClass(HonorRoll.class);
        verify(honorRollGenerator).generate(captor.capture());
        assertThat(captor.getValue().rows()).isEmpty();
    }

    /**
     * Classement standard (§maquette "Tableau d'honneur.dc.html", EX ÆQUO) : deux moyennes
     * strictement égales partagent le même rang ("1, 1, 3"), jamais "1, 2, 3" — sinon le 2e élève à
     * égalité avec le 1er se retrouverait classé en dessous d'un 3e qui a une moyenne strictement
     * inférieure, ce qui serait faux.
     */
    @Test
    void honorRollPdf_tiedAverages_shareTheSameRankAndAreFlaggedExAequo() {
        TenantContext.set(4L);
        ReportCard first = card(1L, 101L, 49L, BigDecimal.valueOf(18));
        ReportCard tiedWithFirst = card(2L, 102L, 49L, BigDecimal.valueOf(18));
        ReportCard third = card(3L, 103L, 49L, BigDecimal.valueOf(15));
        when(repo.findByPeriodId(99L)).thenReturn(List.of(first, tiedWithFirst, third));
        when(studentProvider.findStudent(anyLong())).thenReturn(Optional.of(student(1L, "A", "B")));
        when(academicsProvider.findClass(anyLong())).thenReturn(Optional.empty());
        when(academicsProvider.findPeriod(anyLong())).thenReturn(Optional.empty());
        when(honorRollGenerator.generate(any())).thenReturn(new byte[]{1});

        service.honorRollPdf(99L);

        ArgumentCaptor<HonorRoll> captor = ArgumentCaptor.forClass(HonorRoll.class);
        verify(honorRollGenerator).generate(captor.capture());
        List<Row> rows = captor.getValue().rows();

        assertThat(rows).extracting(Row::rank).containsExactly(1, 1, 3);
        assertThat(rows).extracting(Row::exAequo).containsExactly(true, true, false);
    }
}
