package gn.lacaeducation.academics.application.service;

import gn.lacaeducation.academics.application.port.in.GererScheduleExceptionUseCase.EffectiveScheduleSlot;
import gn.lacaeducation.academics.application.port.in.GererScheduleExceptionUseCase.MoveScheduleForWeekCommand;
import gn.lacaeducation.academics.application.port.out.EmploiDuTempsRepository;
import gn.lacaeducation.academics.application.port.out.ScheduleExceptionRepository;
import gn.lacaeducation.academics.domain.EmploiDuTemps;
import gn.lacaeducation.academics.domain.ScheduleException;
import gn.lacaeducation.academics.domain.valueobject.JourSemaine;
import gn.lacaeducation.academics.domain.valueobject.ScheduleExceptionType;
import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.ConflictException;
import gn.lacaeducation.common.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleExceptionServiceTest {

    @Mock EmploiDuTempsRepository edtRepository;
    @Mock ScheduleExceptionRepository exceptionRepository;
    @InjectMocks ScheduleExceptionService service;

    private static final LocalDate MONDAY = LocalDate.of(2026, 9, 7); // un vrai lundi
    private static final LocalDate TUESDAY = LocalDate.of(2026, 9, 8);

    private EmploiDuTemps original() {
        return EmploiDuTemps.builder().id(1L).tenantId(10L).schoolClassId(100L).subjectId(200L)
                .teacherId(300L).roomId(400L).day(JourSemaine.LUNDI)
                .startTime(LocalTime.of(8, 0)).endTime(LocalTime.of(8, 30)).build();
    }

    private MoveScheduleForWeekCommand moveCmd() {
        return new MoveScheduleForWeekCommand(10L, 1L, MONDAY, JourSemaine.MARDI,
                LocalTime.of(9, 0), LocalTime.of(9, 30), null, null, null);
    }

    @Test
    void moveForWeek_weekStartDateNotMonday_throwsBusinessRuleException() {
        when(edtRepository.findById(1L)).thenReturn(Optional.of(original()));
        MoveScheduleForWeekCommand cmd = new MoveScheduleForWeekCommand(10L, 1L, TUESDAY,
                JourSemaine.MARDI, LocalTime.of(9, 0), LocalTime.of(9, 30), null, null, null);

        assertThatThrownBy(() -> service.moveForWeek(cmd)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void moveForWeek_invalidTimeRange_throwsBusinessRuleException() {
        when(edtRepository.findById(1L)).thenReturn(Optional.of(original()));
        MoveScheduleForWeekCommand cmd = new MoveScheduleForWeekCommand(10L, 1L, MONDAY,
                JourSemaine.MARDI, LocalTime.of(10, 0), LocalTime.of(9, 0), null, null, null);

        assertThatThrownBy(() -> service.moveForWeek(cmd)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void moveForWeek_originalScheduleNotFound_throwsResourceNotFoundException() {
        when(edtRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.moveForWeek(moveCmd())).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void moveForWeek_roomConflictWithAnotherRecurrentSlotThatWeek_throwsConflictException() {
        when(edtRepository.findById(1L)).thenReturn(Optional.of(original()));
        when(exceptionRepository.findByOriginalScheduleIdAndWeek(1L, MONDAY)).thenReturn(Optional.empty());
        when(exceptionRepository.findByWeek(MONDAY)).thenReturn(List.of());
        EmploiDuTemps otherInSameRoom = EmploiDuTemps.builder().id(2L).schoolClassId(999L).roomId(400L)
                .day(JourSemaine.MARDI).startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(9, 30)).build();
        when(edtRepository.search(null, null, 400L, JourSemaine.MARDI)).thenReturn(List.of(otherInSameRoom));

        assertThatThrownBy(() -> service.moveForWeek(moveCmd()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("salle");
    }

    @Test
    void moveForWeek_roomConflictWithAnotherExceptionAlreadyMovedThatWeek_throwsConflictException() {
        when(edtRepository.findById(1L)).thenReturn(Optional.of(original()));
        when(exceptionRepository.findByOriginalScheduleIdAndWeek(1L, MONDAY)).thenReturn(Optional.empty());
        when(edtRepository.search(null, null, 400L, JourSemaine.MARDI)).thenReturn(List.of());
        when(edtRepository.search(null, 300L, null, JourSemaine.MARDI)).thenReturn(List.of());
        when(edtRepository.search(100L, null, null, JourSemaine.MARDI)).thenReturn(List.of());

        ScheduleException otherException = ScheduleException.builder().id(55L).originalScheduleId(9L)
                .type(ScheduleExceptionType.MOVED).newDay(JourSemaine.MARDI)
                .newStartTime(LocalTime.of(9, 0)).newEndTime(LocalTime.of(9, 30)).build();
        when(exceptionRepository.findByWeek(MONDAY)).thenReturn(List.of(otherException));
        EmploiDuTemps otherOriginal = EmploiDuTemps.builder().id(9L).schoolClassId(777L).roomId(400L)
                .day(JourSemaine.JEUDI).startTime(LocalTime.of(11, 0)).endTime(LocalTime.of(11, 30)).build();
        when(edtRepository.findById(9L)).thenReturn(Optional.of(otherOriginal));

        assertThatThrownBy(() -> service.moveForWeek(moveCmd())).isInstanceOf(ConflictException.class);
    }

    @Test
    void moveForWeek_noConflict_createsException() {
        when(edtRepository.findById(1L)).thenReturn(Optional.of(original()));
        when(exceptionRepository.findByOriginalScheduleIdAndWeek(1L, MONDAY)).thenReturn(Optional.empty());
        when(exceptionRepository.findByWeek(MONDAY)).thenReturn(List.of());
        when(edtRepository.search(any(), any(), any(), any())).thenReturn(List.of());
        when(exceptionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ScheduleException result = service.moveForWeek(moveCmd());

        assertThat(result.getType()).isEqualTo(ScheduleExceptionType.MOVED);
        assertThat(result.getNewDay()).isEqualTo(JourSemaine.MARDI);
        assertThat(result.getOriginalScheduleId()).isEqualTo(1L);
    }

    @Test
    void moveForWeek_calledTwiceSameWeek_updatesExistingExceptionInsteadOfDuplicating() {
        when(edtRepository.findById(1L)).thenReturn(Optional.of(original()));
        ScheduleException existing = ScheduleException.builder().id(77L).tenantId(10L)
                .originalScheduleId(1L).weekStartDate(MONDAY).type(ScheduleExceptionType.MOVED)
                .newDay(JourSemaine.MERCREDI).newStartTime(LocalTime.of(10, 0)).newEndTime(LocalTime.of(10, 30)).build();
        when(exceptionRepository.findByOriginalScheduleIdAndWeek(1L, MONDAY)).thenReturn(Optional.of(existing));
        when(exceptionRepository.findByWeek(MONDAY)).thenReturn(List.of(existing));
        when(edtRepository.search(any(), any(), any(), any())).thenReturn(List.of());
        when(exceptionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ArgumentCaptor<ScheduleException> captor = ArgumentCaptor.forClass(ScheduleException.class);
        service.moveForWeek(moveCmd());
        verify(exceptionRepository).save(captor.capture());

        assertThat(captor.getValue().getId()).isEqualTo(77L); // id conservé : mise à jour, pas duplication
        assertThat(captor.getValue().getNewDay()).isEqualTo(JourSemaine.MARDI); // nouvelle cible appliquée
    }

    @Test
    void moveForWeek_conflictIgnoredIfConflictingSlotItselfHasBeenMovedAwayThatWeek() {
        when(edtRepository.findById(1L)).thenReturn(Optional.of(original()));
        when(exceptionRepository.findByOriginalScheduleIdAndWeek(1L, MONDAY)).thenReturn(Optional.empty());

        // Un autre créneau récurrent occupe la même salle/jour/heure...
        EmploiDuTemps conflicting = EmploiDuTemps.builder().id(2L).schoolClassId(999L).roomId(400L)
                .day(JourSemaine.MARDI).startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(9, 30)).build();
        when(edtRepository.search(null, null, 400L, JourSemaine.MARDI)).thenReturn(List.of(conflicting));
        when(edtRepository.search(null, 300L, null, JourSemaine.MARDI)).thenReturn(List.of());
        when(edtRepository.search(100L, null, null, JourSemaine.MARDI)).thenReturn(List.of());
        // ...mais CE créneau (id=2) a lui-même une exception cette semaine (il est déplacé ailleurs) :
        ScheduleException conflictingIsMovedAway = ScheduleException.builder().id(88L).originalScheduleId(2L)
                .type(ScheduleExceptionType.MOVED).newDay(JourSemaine.VENDREDI)
                .newStartTime(LocalTime.of(14, 0)).newEndTime(LocalTime.of(14, 30)).build();
        when(exceptionRepository.findByWeek(MONDAY)).thenReturn(List.of(conflictingIsMovedAway));
        when(exceptionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ScheduleException result = service.moveForWeek(moveCmd()); // ne doit PAS lever de conflit

        assertThat(result.getType()).isEqualTo(ScheduleExceptionType.MOVED);
    }

    @Test
    void cancelException_removesRow() {
        service.cancelException(1L, MONDAY);

        verify(exceptionRepository).deleteByOriginalScheduleIdAndWeek(1L, MONDAY);
    }

    @Test
    void effectiveScheduleForClass_noExceptions_returnsRecurrentSlotsUnchanged() {
        when(edtRepository.findByClassId(100L)).thenReturn(List.of(original()));
        when(exceptionRepository.findByWeek(MONDAY)).thenReturn(List.of());

        List<EffectiveScheduleSlot> result = service.effectiveScheduleForClass(100L, MONDAY);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).moved()).isFalse();
        assertThat(result.get(0).cancelled()).isFalse();
        assertThat(result.get(0).day()).isEqualTo(JourSemaine.LUNDI);
    }

    @Test
    void effectiveScheduleForClass_withMovedException_originalSlotAbsent_newSlotPresentWithMovedTrue() {
        when(edtRepository.findByClassId(100L)).thenReturn(List.of(original()));
        ScheduleException moved = ScheduleException.builder().id(5L).originalScheduleId(1L)
                .type(ScheduleExceptionType.MOVED).newDay(JourSemaine.MARDI)
                .newStartTime(LocalTime.of(9, 0)).newEndTime(LocalTime.of(9, 30)).build();
        when(exceptionRepository.findByWeek(MONDAY)).thenReturn(List.of(moved));

        List<EffectiveScheduleSlot> result = service.effectiveScheduleForClass(100L, MONDAY);

        assertThat(result).hasSize(1);
        EffectiveScheduleSlot slot = result.get(0);
        assertThat(slot.moved()).isTrue();
        assertThat(slot.day()).isEqualTo(JourSemaine.MARDI);
        assertThat(slot.startTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(slot.originalDay()).isEqualTo(JourSemaine.LUNDI);
        assertThat(slot.originalStartTime()).isEqualTo(LocalTime.of(8, 0));
    }

    @Test
    void effectiveScheduleForClass_withCancelledException_slotMarkedCancelledAtOriginalPosition() {
        when(edtRepository.findByClassId(100L)).thenReturn(List.of(original()));
        ScheduleException cancelled = ScheduleException.builder().id(6L).originalScheduleId(1L)
                .type(ScheduleExceptionType.CANCELLED).build();
        when(exceptionRepository.findByWeek(MONDAY)).thenReturn(List.of(cancelled));

        List<EffectiveScheduleSlot> result = service.effectiveScheduleForClass(100L, MONDAY);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).cancelled()).isTrue();
        assertThat(result.get(0).moved()).isFalse();
        assertThat(result.get(0).day()).isEqualTo(JourSemaine.LUNDI); // reste affiché à sa place d'origine
    }

    @Test
    void effectiveScheduleForTeacher_includesSlotsMovedIntoThisTeacherThisWeek() {
        when(edtRepository.findByTeacherId(500L)).thenReturn(List.of());
        ScheduleException movedToMe = ScheduleException.builder().id(7L).originalScheduleId(1L)
                .type(ScheduleExceptionType.MOVED).newDay(JourSemaine.MARDI)
                .newStartTime(LocalTime.of(9, 0)).newEndTime(LocalTime.of(9, 30)).newTeacherId(500L).build();
        when(exceptionRepository.findByWeek(MONDAY)).thenReturn(List.of(movedToMe));
        when(edtRepository.findById(1L)).thenReturn(Optional.of(original())); // teacherId d'origine = 300L

        List<EffectiveScheduleSlot> result = service.effectiveScheduleForTeacher(500L, MONDAY);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).teacherId()).isEqualTo(500L);
        assertThat(result.get(0).moved()).isTrue();
    }

    @Test
    void effectiveScheduleForTeacher_excludesSlotsMovedAwayFromThisTeacherThisWeek() {
        when(edtRepository.findByTeacherId(300L)).thenReturn(List.of(original())); // teacherId=300L
        ScheduleException movedAway = ScheduleException.builder().id(8L).originalScheduleId(1L)
                .type(ScheduleExceptionType.MOVED).newDay(JourSemaine.MARDI)
                .newStartTime(LocalTime.of(9, 0)).newEndTime(LocalTime.of(9, 30)).newTeacherId(600L).build();
        when(exceptionRepository.findByWeek(MONDAY)).thenReturn(List.of(movedAway));

        List<EffectiveScheduleSlot> result = service.effectiveScheduleForTeacher(300L, MONDAY);

        assertThat(result).isEmpty();
    }
}
