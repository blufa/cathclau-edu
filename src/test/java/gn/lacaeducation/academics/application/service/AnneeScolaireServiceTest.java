package gn.lacaeducation.academics.application.service;

import gn.lacaeducation.academics.application.event.AnneeScolaireArchiveeEvent;
import gn.lacaeducation.academics.application.event.AnneeScolaireClotureeEvent;
import gn.lacaeducation.academics.application.event.PeriodeClotureeEvent;
import gn.lacaeducation.academics.application.port.in.GererAnneeScolaireUseCase.CreateAcademicYearCommand;
import gn.lacaeducation.academics.application.port.in.GererAnneeScolaireUseCase.CreatePeriodCommand;
import gn.lacaeducation.academics.application.port.out.AnneeScolaireRepository;
import gn.lacaeducation.academics.application.port.out.PeriodeRepository;
import gn.lacaeducation.academics.domain.AnneeScolaire;
import gn.lacaeducation.academics.domain.Periode;
import gn.lacaeducation.academics.domain.valueobject.SchoolCycle;
import gn.lacaeducation.academics.domain.valueobject.StatutAnneeScolaire;
import gn.lacaeducation.academics.domain.valueobject.TypePeriode;
import gn.lacaeducation.common.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnneeScolaireServiceTest {

    @Mock AnneeScolaireRepository anneeRepository;
    @Mock PeriodeRepository periodeRepository;
    @Mock ApplicationEventPublisher eventPublisher;
    @InjectMocks AnneeScolaireService service;

    @Test
    void create_newAcademicYear_statusPlannedAndNotCurrent() {
        AnneeScolaire expected = AnneeScolaire.builder()
                .id(1L).tenantId(10L).label("2025-2026")
                .status(StatutAnneeScolaire.PLANIFIEE).current(false).build();
        when(anneeRepository.save(any())).thenReturn(expected);

        AnneeScolaire result = service.create(new CreateAcademicYearCommand(
                10L, "2025-2026",
                LocalDate.of(2025, 9, 1), LocalDate.of(2026, 6, 30)
        ));

        assertThat(result.getStatus()).isEqualTo(StatutAnneeScolaire.PLANIFIEE);
        assertThat(result.getCurrent()).isFalse();
    }

    @Test
    void start_academicYear_becomesCurrentAndActive() {
        AnneeScolaire annee = AnneeScolaire.builder()
                .id(1L).tenantId(10L).status(StatutAnneeScolaire.PLANIFIEE).current(false).build();
        when(anneeRepository.findById(1L)).thenReturn(Optional.of(annee));
        when(anneeRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AnneeScolaire result = service.start(1L);

        assertThat(result.getStatus()).isEqualTo(StatutAnneeScolaire.EN_COURS);
        assertThat(result.getCurrent()).isTrue();
        verify(anneeRepository).removeCurrent(10L);
    }

    @Test
    void closePeriod_publishesEvent() {
        Periode periode = Periode.builder()
                .id(5L).academicYearId(1L).label("T1").closed(false).build();
        AnneeScolaire annee = AnneeScolaire.builder()
                .id(1L).tenantId(10L).build();

        when(periodeRepository.findById(5L)).thenReturn(Optional.of(periode));
        when(periodeRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(anneeRepository.findById(1L)).thenReturn(Optional.of(annee));

        service.closePeriod(5L);

        ArgumentCaptor<PeriodeClotureeEvent> captor = ArgumentCaptor.forClass(PeriodeClotureeEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().tenantId()).isEqualTo(10L);
        assertThat(captor.getValue().periodId()).isEqualTo(5L);
    }

    @Test
    void closePeriod_alreadyClosed_throwsException() {
        Periode periode = Periode.builder().id(5L).academicYearId(1L).closed(true).build();
        when(periodeRepository.findById(5L)).thenReturn(Optional.of(periode));

        assertThatThrownBy(() -> service.closePeriod(5L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already closed");
    }

    @Test
    void createPeriod_persistsWithClosedFalse() {
        when(periodeRepository.save(any())).thenAnswer(i -> {
            Periode p = i.getArgument(0);
            p.setId(10L);
            return p;
        });

        Periode result = service.createPeriod(  new CreatePeriodCommand(
                1L, "Term 1", TypePeriode.TRIMESTRE, SchoolCycle.PRIMAIRE,1, LocalDate.of(2020,6,2),LocalDate.of(2020,6,3), null
        ));

        assertThat(result.getClosed()).isFalse();
        assertThat(result.getLabel()).isEqualTo("Term 1");
    }

    @Test
    void setCurrent_flipsPreviousCurrentOffAndSetsThisOne() {
        AnneeScolaire annee = AnneeScolaire.builder()
                .id(2L).tenantId(10L).status(StatutAnneeScolaire.EN_COURS).current(false).build();
        when(anneeRepository.findById(2L)).thenReturn(Optional.of(annee));
        when(anneeRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AnneeScolaire result = service.setCurrent(2L);

        verify(anneeRepository).removeCurrent(10L);
        assertThat(result.getCurrent()).isTrue();
    }

    @Test
    void update_archivedYear_throws() {
        AnneeScolaire annee = AnneeScolaire.builder()
                .id(1L).tenantId(10L).status(StatutAnneeScolaire.ARCHIVEE).build();
        when(anneeRepository.findById(1L)).thenReturn(Optional.of(annee));

        assertThatThrownBy(() -> service.update(1L, new CreateAcademicYearCommand(
                10L, "x", LocalDate.now(), LocalDate.now().plusDays(1))))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void close_publishesClotureeEvent() {
        AnneeScolaire annee = AnneeScolaire.builder()
                .id(1L).tenantId(10L).status(StatutAnneeScolaire.EN_COURS).current(true).build();
        when(anneeRepository.findById(1L)).thenReturn(Optional.of(annee));
        when(periodeRepository.findByAcademicYearId(1L)).thenReturn(List.of());
        when(anneeRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.close(1L);

        ArgumentCaptor<AnneeScolaireClotureeEvent> captor = ArgumentCaptor.forClass(AnneeScolaireClotureeEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().academicYearId()).isEqualTo(1L);
    }

    @Test
    void archive_publishesArchiveeEvent() {
        AnneeScolaire annee = AnneeScolaire.builder()
                .id(1L).tenantId(10L).status(StatutAnneeScolaire.CLOTUREE).build();
        when(anneeRepository.findById(1L)).thenReturn(Optional.of(annee));
        when(anneeRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.archive(1L);

        ArgumentCaptor<AnneeScolaireArchiveeEvent> captor = ArgumentCaptor.forClass(AnneeScolaireArchiveeEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().academicYearId()).isEqualTo(1L);
    }
}
