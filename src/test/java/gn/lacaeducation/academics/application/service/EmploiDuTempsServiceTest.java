package gn.lacaeducation.academics.application.service;

import gn.lacaeducation.academics.application.port.in.GererEmploiDuTempsUseCase.CreateScheduleCommand;
import gn.lacaeducation.academics.application.port.out.EmploiDuTempsRepository;
import gn.lacaeducation.academics.domain.EmploiDuTemps;
import gn.lacaeducation.academics.domain.valueobject.JourSemaine;
import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.ConflictException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmploiDuTempsServiceTest {

    @Mock EmploiDuTempsRepository repo;
    @InjectMocks EmploiDuTempsService service;

    private CreateScheduleCommand cmd() {
        return new CreateScheduleCommand(10L, 1L, 2L, 3L, 4L,
                JourSemaine.LUNDI, LocalTime.of(8, 0), LocalTime.of(10, 0), null);
    }

    @Test
    void create_roomConflict_throws409() {
        when(repo.existsRoomConflict(4L, JourSemaine.LUNDI, LocalTime.of(8, 0), LocalTime.of(10, 0)))
                .thenReturn(true);

        assertThatThrownBy(() -> service.create(cmd()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Cette salle est déjà occupée");
        verify(repo, never()).save(any());
    }

    @Test
    void create_teacherConflict_throws409() {
        when(repo.existsRoomConflict(any(), any(), any(), any())).thenReturn(false);
        when(repo.existsTeacherConflict(3L, JourSemaine.LUNDI, LocalTime.of(8, 0), LocalTime.of(10, 0)))
                .thenReturn(true);

        assertThatThrownBy(() -> service.create(cmd()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Cet enseignant a déjà un cours");
    }

    @Test
    void create_classConflict_throws409() {
        when(repo.existsRoomConflict(any(), any(), any(), any())).thenReturn(false);
        when(repo.existsTeacherConflict(any(), any(), any(), any())).thenReturn(false);
        when(repo.existsClassConflict(1L, JourSemaine.LUNDI, LocalTime.of(8, 0), LocalTime.of(10, 0)))
                .thenReturn(true);

        assertThatThrownBy(() -> service.create(cmd()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Cette classe a déjà un cours");
    }

    @Test
    void create_invalidTimeRange_throws422() {
        CreateScheduleCommand bad = new CreateScheduleCommand(10L, 1L, 2L, 3L, 4L,
                JourSemaine.LUNDI, LocalTime.of(10, 0), LocalTime.of(8, 0), null);

        assertThatThrownBy(() -> service.create(bad))
                .isInstanceOf(BusinessRuleException.class);
        verify(repo, never()).save(any());
    }

    @Test
    void create_noConflict_persists() {
        when(repo.existsRoomConflict(any(), any(), any(), any())).thenReturn(false);
        when(repo.existsTeacherConflict(any(), any(), any(), any())).thenReturn(false);
        when(repo.existsClassConflict(any(), any(), any(), any())).thenReturn(false);
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        EmploiDuTemps result = service.create(cmd());

        verify(repo).save(any(EmploiDuTemps.class));
        org.assertj.core.api.Assertions.assertThat(result.getDay()).isEqualTo(JourSemaine.LUNDI);
    }
}
