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

/** Même régression que RolePersistenceAdapterTest : ne jamais réinsérer une paire (teacher_id,
 * subject_id) déjà présente, sous peine de violer la contrainte unique de teacher_subjects. */
@ExtendWith(MockitoExtension.class)
class TeacherSubjectPersistenceAdapterTest {

    @Mock TeacherSubjectJpaRepository jpa;

    TeacherSubjectPersistenceAdapter adapter() { return new TeacherSubjectPersistenceAdapter(jpa); }

    private TeacherSubjectJpaEntity existing(Long teacherId, Long subjectId) {
        TeacherSubjectJpaEntity e = new TeacherSubjectJpaEntity();
        e.setTeacherId(teacherId);
        e.setSubjectId(subjectId);
        return e;
    }

    @Test
    @SuppressWarnings("unchecked")
    void replace_neverReinsertsAnAlreadyAssignedSubject() {
        when(jpa.findByTeacherId(4L)).thenReturn(List.of(existing(4L, 7L)));

        adapter().replace(4L, 1L, List.of(7L, 9L));

        ArgumentCaptor<TeacherSubjectJpaEntity> savedCaptor = ArgumentCaptor.forClass(TeacherSubjectJpaEntity.class);
        verify(jpa).save(savedCaptor.capture());
        assertThat(savedCaptor.getValue().getSubjectId()).isEqualTo(9L);

        ArgumentCaptor<List<TeacherSubjectJpaEntity>> removedCaptor = ArgumentCaptor.forClass(List.class);
        verify(jpa).deleteAll(removedCaptor.capture());
        assertThat(removedCaptor.getValue()).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void replace_removesOnlySubjectsNoLongerRequested() {
        when(jpa.findByTeacherId(4L)).thenReturn(List.of(existing(4L, 7L), existing(4L, 8L)));

        adapter().replace(4L, 1L, List.of(7L));

        ArgumentCaptor<List<TeacherSubjectJpaEntity>> removedCaptor = ArgumentCaptor.forClass(List.class);
        verify(jpa).deleteAll(removedCaptor.capture());
        assertThat(removedCaptor.getValue()).extracting(TeacherSubjectJpaEntity::getSubjectId).containsExactly(8L);
        verify(jpa, never()).save(any());
    }
}
