package gn.lacaeducation.academics.application.service;

import gn.lacaeducation.academics.application.port.out.ReferentielRepository;
import gn.lacaeducation.academics.domain.Matiere;
import gn.lacaeducation.academics.domain.referentiel.MatiereModele;
import gn.lacaeducation.academics.domain.valueobject.SchoolCycle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReferentielAcademicsServiceTest {

    @Mock ReferentielRepository referentielRepository;
    @InjectMocks ReferentielAcademicsService service;

    @Test
    void catalogueMepua_college_returnsOfficialCoefficients() {
        List<MatiereModele> catalogue = service.catalogueMepua(SchoolCycle.COLLEGE, null);

        assertThat(catalogue)
                .extracting(MatiereModele::libelle, MatiereModele::coefficient)
                .contains(
                        org.assertj.core.groups.Tuple.tuple("Mathématiques", BigDecimal.valueOf(4)),
                        org.assertj.core.groups.Tuple.tuple("Anglais", BigDecimal.valueOf(2)),
                        org.assertj.core.groups.Tuple.tuple("Histoire", BigDecimal.valueOf(1))
                );
    }

    @Test
    void catalogueMepua_lyceeTerminale_variesCoefficientBySerie() {
        BigDecimal mathSm = service.catalogueMepua(SchoolCycle.LYCEE, "SM").stream()
                .filter(m -> m.libelle().equals("Mathématiques")).findFirst().orElseThrow().coefficient();
        BigDecimal mathSs = service.catalogueMepua(SchoolCycle.LYCEE, "SS").stream()
                .filter(m -> m.libelle().equals("Mathématiques")).findFirst().orElseThrow().coefficient();

        assertThat(mathSm).isEqualByComparingTo("5");
        assertThat(mathSs).isEqualByComparingTo("2");
    }

    @Test
    void catalogueMepua_lyceeWithoutSerie_isEmpty() {
        assertThat(service.catalogueMepua(SchoolCycle.LYCEE, null)).isEmpty();
    }

    @Test
    void importerMatieresMepua_skipsSubjectsAlreadyPresent() {
        when(referentielRepository.findSubjectsByTenantId(10L)).thenReturn(
                List.of(Matiere.builder().id(1L).tenantId(10L).label("Mathématiques").coefficient(BigDecimal.ONE).build())
        );
        when(referentielRepository.saveSubject(any())).thenAnswer(i -> i.getArgument(0));

        List<Matiere> created = service.importerMatieresMepua(10L, SchoolCycle.COLLEGE, null);

        assertThat(created).extracting(Matiere::getLabel).doesNotContain("Mathématiques");
        assertThat(created).hasSize(9); // 10 matières du catalogue Collège - 1 déjà existante
    }

    @Test
    void importerMatieresMepua_unknownCycleOrSerie_createsNothing() {
        List<Matiere> created = service.importerMatieresMepua(10L, SchoolCycle.LYCEE, "INCONNU");

        assertThat(created).isEmpty();
        verify(referentielRepository, never()).saveSubject(any());
    }
}
