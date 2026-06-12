package gn.lacaeducation.academics.application.service;

import gn.lacaeducation.academics.application.port.in.GererClasseUseCase.CreateClassCommand;
import gn.lacaeducation.academics.application.port.out.AnneeScolaireRepository;
import gn.lacaeducation.academics.application.port.out.ClasseRepository;
import gn.lacaeducation.academics.application.port.out.EnrollmentQueryPort;
import gn.lacaeducation.academics.domain.AnneeScolaire;
import gn.lacaeducation.academics.domain.Classe;
import gn.lacaeducation.academics.domain.valueobject.StatutAnneeScolaire;
import gn.lacaeducation.common.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClasseServiceTest {

    @Mock ClasseRepository classeRepository;
    @Mock EnrollmentQueryPort enrollmentQueryPort;
    @Mock AnneeScolaireRepository anneeRepository;
    @InjectMocks ClasseService service;

    private CreateClassCommand cmd(Long yearId) {
        return new CreateClassCommand(10L, 1L, yearId, 2L, 3L, "6e A", 40);
    }

    @Test
    void create_onClosedYear_throws() {
        when(anneeRepository.findById(5L)).thenReturn(Optional.of(
                AnneeScolaire.builder().id(5L).status(StatutAnneeScolaire.CLOTUREE).build()));

        assertThatThrownBy(() -> service.create(cmd(5L)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("read-only");
        verify(classeRepository, never()).save(any());
    }

    @Test
    void create_onActiveYear_persists() {
        when(anneeRepository.findById(5L)).thenReturn(Optional.of(
                AnneeScolaire.builder().id(5L).status(StatutAnneeScolaire.EN_COURS).build()));
        when(classeRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Classe result = service.create(cmd(5L));

        verify(classeRepository).save(any(Classe.class));
        assertThat(result.getHeadcount()).isZero();
    }
}
