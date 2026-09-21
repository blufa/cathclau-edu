package gn.lacaeducation.academics.adapters.out.persistence;

import gn.lacaeducation.academics.ClassSubjectPublicInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Depuis 2025, le coefficient d'une matière se surcharge à l'affectation classe/matière
 * (ClasseMatiere#coefficient) — notamment pour les matières de spécialité par série au lycée, dont le
 * coefficient diffère du coefficient générique de la matière. Ces tests couvrent exactement ce
 * mécanisme de repli : surcharge par classe si présente, sinon coefficient générique de la matière.
 */
@ExtendWith(MockitoExtension.class)
class AcademicsDataProviderAdapterTest {

    @Mock ClasseJpaRepository classeRepo;
    @Mock PeriodeJpaRepository periodeRepo;
    @Mock ClasseMatiereJpaRepository classeMatiereRepo;
    @Mock MatiereJpaRepository matiereRepo;
    @Mock EmploiDuTempsJpaRepository emploiDuTempsRepo;
    @Mock AnneeScolaireJpaRepository anneeRepo;
    @Mock NiveauJpaRepository niveauRepo;
    @InjectMocks AcademicsDataProviderAdapter adapter;

    private ClasseMatiereJpaEntity assignment(Long subjectId, BigDecimal coefficient) {
        ClasseMatiereJpaEntity a = new ClasseMatiereJpaEntity();
        a.setSchoolClassId(10L);
        a.setSubjectId(subjectId);
        a.setTeacherId(5L);
        a.setCoefficient(coefficient);
        return a;
    }

    private MatiereJpaEntity subject(Long id, BigDecimal coefficient) {
        MatiereJpaEntity m = new MatiereJpaEntity();
        m.setId(id);
        m.setLabel("Mathématiques");
        m.setCoefficient(coefficient);
        return m;
    }

    @Test
    void findClassSubjects_perClassCoefficient_overridesSubjectDefault() {
        // Ex. Mathématiques, coefficient générique 4, mais 6 dans cette classe (spécialité série S).
        when(classeMatiereRepo.findBySchoolClassId(10L)).thenReturn(List.of(assignment(1L, new BigDecimal("6"))));
        when(matiereRepo.findAllById(java.util.Set.of(1L))).thenReturn(List.of(subject(1L, new BigDecimal("4"))));

        List<ClassSubjectPublicInfo> result = adapter.findClassSubjects(10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).coefficient()).isEqualByComparingTo("6");
    }

    @Test
    void findClassSubjects_noPerClassCoefficient_fallsBackToSubjectDefault() {
        // Cas courant au collège : aucune surcharge saisie, le coefficient générique de la matière
        // s'applique tel quel.
        when(classeMatiereRepo.findBySchoolClassId(10L)).thenReturn(List.of(assignment(1L, null)));
        when(matiereRepo.findAllById(java.util.Set.of(1L))).thenReturn(List.of(subject(1L, new BigDecimal("3"))));

        List<ClassSubjectPublicInfo> result = adapter.findClassSubjects(10L);

        assertThat(result.get(0).coefficient()).isEqualByComparingTo("3");
    }

    @Test
    void findClassSubjects_neitherCoefficientSet_defaultsToOne() {
        when(classeMatiereRepo.findBySchoolClassId(10L)).thenReturn(List.of(assignment(1L, null)));
        when(matiereRepo.findAllById(java.util.Set.of(1L))).thenReturn(List.of(subject(1L, null)));

        List<ClassSubjectPublicInfo> result = adapter.findClassSubjects(10L);

        assertThat(result.get(0).coefficient()).isEqualByComparingTo("1");
    }

    @Test
    void findClassSubjectsByTeacher_perClassCoefficient_overridesSubjectDefault() {
        when(classeMatiereRepo.findByTeacherId(5L)).thenReturn(List.of(assignment(1L, new BigDecimal("2"))));
        when(matiereRepo.findAllById(java.util.Set.of(1L))).thenReturn(List.of(subject(1L, new BigDecimal("4"))));

        List<ClassSubjectPublicInfo> result = adapter.findClassSubjectsByTeacher(5L);

        assertThat(result.get(0).coefficient()).isEqualByComparingTo("2");
    }
}
