package gn.lacaeducation.students.application.service;

import gn.lacaeducation.students.application.port.out.ParentRepository;
import gn.lacaeducation.students.application.port.out.StudentParentRepository;
import gn.lacaeducation.students.domain.Parent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParentAccessProviderAdapterTest {

    @Mock ParentRepository parentRepository;
    @Mock StudentParentRepository studentParentRepository;
    @InjectMocks ParentAccessProviderAdapter adapter;

    @Test
    void isParentOfStudent_linkedChild_returnsTrue() {
        when(parentRepository.findByUserId(7L)).thenReturn(Optional.of(Parent.builder().id(3L).build()));
        when(studentParentRepository.findStudentIdsByParentId(3L)).thenReturn(List.of(10L, 11L));

        assertThat(adapter.isParentOfStudent(7L, 11L)).isTrue();
    }

    @Test
    void isParentOfStudent_otherChild_returnsFalse() {
        when(parentRepository.findByUserId(7L)).thenReturn(Optional.of(Parent.builder().id(3L).build()));
        when(studentParentRepository.findStudentIdsByParentId(3L)).thenReturn(List.of(10L, 11L));

        assertThat(adapter.isParentOfStudent(7L, 99L)).isFalse();
    }

    @Test
    void isParentOfStudent_notAParent_returnsFalse() {
        when(parentRepository.findByUserId(7L)).thenReturn(Optional.empty());

        assertThat(adapter.isParentOfStudent(7L, 11L)).isFalse();
    }

    @Test
    void isParentOfStudent_nullArgs_returnsFalse() {
        assertThat(adapter.isParentOfStudent(null, 11L)).isFalse();
    }
}
