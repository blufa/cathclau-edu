package gn.lacaeducation.academics.application.service;

import gn.lacaeducation.academics.application.port.in.GererEmploiDuTempsUseCase.CreateScheduleCommand;
import gn.lacaeducation.academics.application.port.out.EmploiDuTempsRepository;
import gn.lacaeducation.academics.domain.EmploiDuTemps;
import gn.lacaeducation.academics.domain.valueobject.JourSemaine;
import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.ConflictException;
import gn.lacaeducation.academics.application.port.out.TeacherScheduleFlagPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmploiDuTempsServiceTest {

    @Mock EmploiDuTempsRepository repo;
    @Mock TeacherScheduleFlagPort teacherProvider;
    @InjectMocks EmploiDuTempsService service;

    @BeforeEach
    void defaultTeacherDoesNotAllowConcurrentClasses() {
        lenient().when(teacherProvider.allowsConcurrentClasses(any())).thenReturn(false);
    }

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

    /** Enseignant exceptionnel (ex. EPS) pouvant couvrir plusieurs classes en même temps (§5.6) : le
     * conflit d'enseignant est ignoré pour lui précisément, sans jamais désactiver les conflits de
     * salle/classe. */
    @Test
    void create_teacherAllowsConcurrentClasses_bypassesTeacherConflict() {
        when(teacherProvider.allowsConcurrentClasses(3L)).thenReturn(true);
        when(repo.existsRoomConflict(any(), any(), any(), any())).thenReturn(false);
        when(repo.existsClassConflict(any(), any(), any(), any())).thenReturn(false);
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        EmploiDuTemps result = service.create(cmd());

        verify(repo, never()).existsTeacherConflict(any(), any(), any(), any());
        verify(repo).save(any(EmploiDuTemps.class));
        org.assertj.core.api.Assertions.assertThat(result.getDay()).isEqualTo(JourSemaine.LUNDI);
    }

    /**
     * §3.1 cahier des charges — bug réel : sans salle assignée (roomId=null, courant au
     * Primaire/Maternelle), `existsRoomConflict(null, ...)` ne doit JAMAIS être appelé, sinon la
     * comparaison `= :roomId` liée à NULL est réécrite en HQL en `IS NULL`, ce qui ferait matcher
     * n'importe quel AUTRE créneau lui aussi sans salle au même horaire — y compris dans un cycle
     * totalement différent (ex. Primaire vs 7ème), un faux conflit sans aucune salle réellement
     * partagée.
     */
    @Test
    void create_nullRoomId_neverCallsExistsRoomConflict() {
        CreateScheduleCommand noRoom = new CreateScheduleCommand(10L, 1L, 2L, 3L, null,
                JourSemaine.LUNDI, LocalTime.of(8, 0), LocalTime.of(10, 0), null);
        when(repo.existsTeacherConflict(any(), any(), any(), any())).thenReturn(false);
        when(repo.existsClassConflict(any(), any(), any(), any())).thenReturn(false);
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        service.create(noRoom);

        verify(repo, never()).existsRoomConflict(any(), any(), any(), any());
        verify(repo, never()).existsRoomConflictExcluding(any(), any(), any(), any(), any());
    }

    /**
     * Scénario littéral du cahier des charges : un créneau Primaire (classId=100) et un créneau
     * Secondaire (classId=200) se chevauchent EXACTEMENT sur l'horaire, mais ne partagent ni salle
     * (toutes deux sans salle assignée), ni enseignant, ni classe — la création du second ne doit
     * jamais être rejetée. Les vrais conflits au sein d'un même cycle (même salle/enseignant/classe,
     * voir les tests ci-dessus) restent, eux, détectés normalement.
     */
    @Test
    void create_overlappingSlotDifferentCycleNoSharedResource_persistsWithoutConflict() {
        CreateScheduleCommand primaryNoRoom = new CreateScheduleCommand(10L, 100L, 50L, 7L, null,
                JourSemaine.LUNDI, LocalTime.of(8, 0), LocalTime.of(10, 0), null);
        CreateScheduleCommand secondaryNoRoomOverlapping = new CreateScheduleCommand(10L, 200L, 60L, 8L, null,
                JourSemaine.LUNDI, LocalTime.of(8, 30), LocalTime.of(9, 30), null);
        when(repo.existsTeacherConflict(any(), any(), any(), any())).thenReturn(false);
        when(repo.existsClassConflict(any(), any(), any(), any())).thenReturn(false);
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        service.create(primaryNoRoom);
        EmploiDuTemps result = service.create(secondaryNoRoomOverlapping);

        org.assertj.core.api.Assertions.assertThat(result.getSchoolClassId()).isEqualTo(200L);
        verify(repo, never()).existsRoomConflict(any(), any(), any(), any());
    }

    @Test
    void create_teacherAllowsConcurrentClasses_stillChecksRoomAndClassConflicts() {
        when(teacherProvider.allowsConcurrentClasses(3L)).thenReturn(true);
        when(repo.existsRoomConflict(any(), any(), any(), any())).thenReturn(false);
        when(repo.existsClassConflict(1L, JourSemaine.LUNDI, LocalTime.of(8, 0), LocalTime.of(10, 0)))
                .thenReturn(true);

        assertThatThrownBy(() -> service.create(cmd()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Cette classe a déjà un cours");
    }
}
