package gn.lacaeducation.staff.adapters.out.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeacherLevelPersistenceAdapterTest {

    @Mock TeacherLevelJpaRepository jpa;

    TeacherLevelPersistenceAdapter adapter() { return new TeacherLevelPersistenceAdapter(jpa); }

    private TeacherLevelJpaEntity existing(Long teacherId, Long levelId) {
        TeacherLevelJpaEntity e = new TeacherLevelJpaEntity();
        e.setTeacherId(teacherId);
        e.setLevelId(levelId);
        return e;
    }

    @Test
    @SuppressWarnings("unchecked")
    void replace_neverReinsertsAnAlreadyAssignedLevel() {
        when(jpa.findByTeacherId(4L)).thenReturn(List.of(existing(4L, 7L)));

        adapter().replace(4L, 1L, List.of(7L, 9L));

        ArgumentCaptor<TeacherLevelJpaEntity> savedCaptor = ArgumentCaptor.forClass(TeacherLevelJpaEntity.class);
        verify(jpa).save(savedCaptor.capture());
        assertThat(savedCaptor.getValue().getLevelId()).isEqualTo(9L);

        ArgumentCaptor<List<TeacherLevelJpaEntity>> removedCaptor = ArgumentCaptor.forClass(List.class);
        verify(jpa).deleteAll(removedCaptor.capture());
        assertThat(removedCaptor.getValue()).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void replace_removesOnlyLevelsNoLongerRequested() {
        when(jpa.findByTeacherId(4L)).thenReturn(List.of(existing(4L, 7L), existing(4L, 8L)));

        adapter().replace(4L, 1L, List.of(7L));

        ArgumentCaptor<List<TeacherLevelJpaEntity>> removedCaptor = ArgumentCaptor.forClass(List.class);
        verify(jpa).deleteAll(removedCaptor.capture());
        assertThat(removedCaptor.getValue()).extracting(TeacherLevelJpaEntity::getLevelId).containsExactly(8L);
        verify(jpa, never()).save(any());
    }
}
