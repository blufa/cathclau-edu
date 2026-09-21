package gn.lacaeducation.billing.application.service;

import gn.lacaeducation.billing.application.port.in.FamilyDiscountUseCase.ApplicableDiscount;
import gn.lacaeducation.billing.application.port.in.FamilyDiscountUseCase.RuleCommand;
import gn.lacaeducation.billing.application.port.out.EnrollmentQueryPort;
import gn.lacaeducation.billing.application.port.out.EnrollmentQueryPort.ActiveLevel;
import gn.lacaeducation.billing.application.port.out.FamilyDiscountRuleRepository;
import gn.lacaeducation.billing.domain.FamilyDiscountRule;
import gn.lacaeducation.billing.domain.FeeType;
import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.students.FamilyProvider;
import gn.lacaeducation.students.FamilyProvider.ChildInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FamilyDiscountServiceTest {

    @Mock FamilyDiscountRuleRepository repo;
    @Mock FamilyProvider familyProvider;
    @Mock EnrollmentQueryPort enrollmentQueryPort;
    @InjectMocks FamilyDiscountService service;

    // Fratrie de 5 enfants ; le plus jeune (date la plus récente) est l'élève 10.
    private List<ChildInfo> fiveChildren() {
        return List.of(
                new ChildInfo(1L, LocalDate.of(2010, 1, 1)),
                new ChildInfo(2L, LocalDate.of(2012, 1, 1)),
                new ChildInfo(3L, LocalDate.of(2014, 1, 1)),
                new ChildInfo(11L, LocalDate.of(2016, 1, 1)),
                new ChildInfo(10L, LocalDate.of(2019, 1, 1)));
    }

    // Niveau au libellé non reconnu par NiveauCanonicalOrder : classement replié sur le sort_order
    // brut (1000 + sortOrder), pour tester le classement par sort_order indépendamment du libellé.
    private ActiveLevel niveauNonReconnu(int sortOrder) {
        return new ActiveLevel("PRIMAIRE", "Niveau X", sortOrder);
    }

    private FamilyDiscountRule rule5(int min, Integer max, String pct) {
        return FamilyDiscountRule.builder().id(1L).label("Règle fratrie").minChildren(min).maxChildren(max)
                .percentage(new BigDecimal(pct)).feeTypes(List.of(FeeType.MONTHLY)).appliesToYoungest(true).active(true).build();
    }

    @Test
    void computeApplicable_exactly5Children_youngestGets5Percent() {
        when(familyProvider.findSiblings(10L)).thenReturn(fiveChildren());
        when(repo.findByTenantId(1L)).thenReturn(List.of(rule5(5, 5, "5")));

        ApplicableDiscount d = service.computeApplicable(1L, 10L, FeeType.MONTHLY);

        assertThat(d.percentage()).isEqualByComparingTo("5");
        assertThat(d.ruleLabel()).isEqualTo("Règle fratrie");
    }

    @Test
    void computeApplicable_notYoungestChild_getsNothing() {
        when(familyProvider.findSiblings(11L)).thenReturn(fiveChildren());
        when(repo.findByTenantId(1L)).thenReturn(List.of(rule5(5, 5, "5")));

        ApplicableDiscount d = service.computeApplicable(1L, 11L, FeeType.MONTHLY);

        assertThat(d.percentage()).isEqualByComparingTo("0");
        assertThat(d.ruleLabel()).isNull();
    }

    @Test
    void computeApplicable_moreThan5Children_youngestFullyExempt() {
        List<ChildInfo> six = new java.util.ArrayList<>(fiveChildren());
        six.add(new ChildInfo(20L, LocalDate.of(2021, 1, 1))); // nouveau plus jeune
        when(familyProvider.findSiblings(20L)).thenReturn(six);
        // Règle « plus de 5 » : min=6, pas de max, 100 %.
        when(repo.findByTenantId(1L)).thenReturn(List.of(rule5(6, null, "100")));

        ApplicableDiscount d = service.computeApplicable(1L, 20L, FeeType.MONTHLY);

        assertThat(d.percentage()).isEqualByComparingTo("100");
    }

    @Test
    void computeApplicable_noMatchingRule_returnsZero() {
        when(familyProvider.findSiblings(10L)).thenReturn(fiveChildren());
        when(repo.findByTenantId(1L)).thenReturn(List.of(rule5(6, null, "100"))); // exige 6 enfants

        ApplicableDiscount d = service.computeApplicable(1L, 10L, FeeType.MONTHLY);

        assertThat(d.percentage()).isEqualByComparingTo("0");
    }

    @Test
    void eligibleYoungest_returnsOnlyTheYoungestAmongMatches() {
        when(familyProvider.findSiblings(10L)).thenReturn(fiveChildren());
        when(familyProvider.findSiblings(1L)).thenReturn(fiveChildren());
        when(repo.findByTenantId(1L)).thenReturn(List.of(rule5(5, 5, "5")));

        List<Long> eligible = service.eligibleYoungest(1L, List.of(10L, 1L));

        assertThat(eligible).containsExactly(10L);
    }

    @Test
    void eligibleYoungest_noRuleAppliesToYoungest_returnsEmpty() {
        FamilyDiscountRule ruleForAll = FamilyDiscountRule.builder().id(2L).label("Toute la fratrie")
                .minChildren(5).maxChildren(5).percentage(new BigDecimal("5"))
                .feeTypes(List.of(FeeType.MONTHLY)).appliesToYoungest(false).active(true).build();
        when(repo.findByTenantId(1L)).thenReturn(List.of(ruleForAll));

        List<Long> eligible = service.eligibleYoungest(1L, List.of(10L));

        assertThat(eligible).isEmpty();
    }

    @Test
    void eligibleYoungest_wrongSchoolSortOrder_canonicalLabelStillPicksOnlyTheYoungest() {
        // Régression : l'école a mal saisi le sort_order de ses niveaux de maternelle (Grande Section
        // sort_order=1, donc "plus bas" que Moyenne Section sort_order=2 selon l'école) — sans le
        // classement par libellé canonique, les DEUX enfants pouvaient ressortir "cadet" simultanément.
        when(familyProvider.findSiblings(1L)).thenReturn(fiveChildren());
        when(familyProvider.findSiblings(2L)).thenReturn(fiveChildren());
        when(enrollmentQueryPort.findActiveLevel(1L)).thenReturn(new ActiveLevel("MATERNELLE", "Grande Section", 1));
        when(enrollmentQueryPort.findActiveLevel(2L)).thenReturn(new ActiveLevel("MATERNELLE", "Moyenne Section", 2));
        when(repo.findByTenantId(1L)).thenReturn(List.of(rule5(5, 5, "5")));

        List<Long> eligible = service.eligibleYoungest(1L, List.of(1L, 2L));

        assertThat(eligible).containsExactly(2L); // Moyenne Section = plus jeune, malgré le sort_order inversé
    }

    @Test
    void eligibleYoungest_childCountOutsideRuleRange_returnsEmpty() {
        when(familyProvider.findSiblings(10L)).thenReturn(fiveChildren());
        when(repo.findByTenantId(1L)).thenReturn(List.of(rule5(6, null, "100"))); // exige 6 enfants, on en a 5

        List<Long> eligible = service.eligibleYoungest(1L, List.of(10L));

        assertThat(eligible).isEmpty();
    }

    @Test
    void create_percentageOver100_throws() {
        assertThatThrownBy(() -> service.create(1L,
                new RuleCommand("x", 5, 5, new BigDecimal("120"), List.of(FeeType.MONTHLY), true, true)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void computeApplicable_multipleFeeTypes_appliesToEachOfThem() {
        FamilyDiscountRule multi = FamilyDiscountRule.builder().id(1L).label("Règle fratrie")
                .minChildren(5).maxChildren(5).percentage(new BigDecimal("5"))
                .feeTypes(List.of(FeeType.MONTHLY, FeeType.TRANSPORT))
                .appliesToYoungest(true).active(true).build();
        when(familyProvider.findSiblings(10L)).thenReturn(fiveChildren());
        when(repo.findByTenantId(1L)).thenReturn(List.of(multi));

        assertThat(service.computeApplicable(1L, 10L, FeeType.MONTHLY).percentage()).isEqualByComparingTo("5");
        assertThat(service.computeApplicable(1L, 10L, FeeType.TRANSPORT).percentage()).isEqualByComparingTo("5");
        assertThat(service.computeApplicable(1L, 10L, FeeType.CANTEEN).percentage()).isEqualByComparingTo("0");
    }

    // ── Cadet déterminé par niveau de classe (plus fiable que la date de naissance) ──────────────────

    @Test
    void computeApplicable_youngestByLevel_lowestLevelWinsOverBirthDate() {
        // Élève 10 est le plus jeune par date de naissance (fiveChildren), mais élève 1 (l'aîné par
        // date) est en réalité inscrit dans le niveau le plus bas (ex. redoublement/entrée tardive) —
        // le niveau doit l'emporter sur la date de naissance.
        when(familyProvider.findSiblings(1L)).thenReturn(fiveChildren());
        when(enrollmentQueryPort.findActiveLevel(1L)).thenReturn(niveauNonReconnu(1)); // niveau le plus bas
        when(enrollmentQueryPort.findActiveLevel(2L)).thenReturn(niveauNonReconnu(3));
        when(enrollmentQueryPort.findActiveLevel(3L)).thenReturn(niveauNonReconnu(5));
        when(enrollmentQueryPort.findActiveLevel(11L)).thenReturn(niveauNonReconnu(7));
        when(enrollmentQueryPort.findActiveLevel(10L)).thenReturn(niveauNonReconnu(9));
        when(repo.findByTenantId(1L)).thenReturn(List.of(rule5(5, 5, "5")));

        ApplicableDiscount d = service.computeApplicable(1L, 1L, FeeType.MONTHLY);

        assertThat(d.percentage()).isEqualByComparingTo("5");
    }

    @Test
    void computeApplicable_youngestByLevel_tieBrokenByLatestBirthDate() {
        // Deux enfants au même niveau (ex. même classe) : la date de naissance la plus récente départage.
        when(familyProvider.findSiblings(10L)).thenReturn(fiveChildren());
        when(enrollmentQueryPort.findActiveLevel(1L)).thenReturn(niveauNonReconnu(5));
        when(enrollmentQueryPort.findActiveLevel(2L)).thenReturn(niveauNonReconnu(5));
        when(enrollmentQueryPort.findActiveLevel(3L)).thenReturn(niveauNonReconnu(5));
        when(enrollmentQueryPort.findActiveLevel(11L)).thenReturn(niveauNonReconnu(1)); // même niveau le plus bas
        when(enrollmentQueryPort.findActiveLevel(10L)).thenReturn(niveauNonReconnu(1)); // que l'élève 10 (né plus tard)
        when(repo.findByTenantId(1L)).thenReturn(List.of(rule5(5, 5, "5")));

        // Élève 10 (né en 2019, plus tard que l'élève 11 né en 2016) gagne le départage.
        assertThat(service.computeApplicable(1L, 10L, FeeType.MONTHLY).percentage()).isEqualByComparingTo("5");
        assertThat(service.computeApplicable(1L, 11L, FeeType.MONTHLY).percentage()).isEqualByComparingTo("0");
    }

    @Test
    void computeApplicable_noEnrollmentDataForAnyChild_fallsBackToBirthDate() {
        // Aucun enfant n'a d'inscription active connue (enrollmentQueryPort non stubbé → null pour
        // tous) : repli complet sur le comportement historique (date de naissance la plus récente).
        when(familyProvider.findSiblings(10L)).thenReturn(fiveChildren());
        when(repo.findByTenantId(1L)).thenReturn(List.of(rule5(5, 5, "5")));

        assertThat(service.computeApplicable(1L, 10L, FeeType.MONTHLY).percentage()).isEqualByComparingTo("5");
    }
}
