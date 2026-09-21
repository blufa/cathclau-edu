package gn.lacaeducation.billing.application.service;

import gn.lacaeducation.billing.application.port.out.EnrollmentQueryPort.ActiveGrade;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NiveauCanonicalOrderTest {

    @Test
    void gradeRank_ordersMaternelleSectionsThenPrimaireThenCollegeThenLycee() {
        assertThat(NiveauCanonicalOrder.gradeRank("PETITE_SECTION"))
                .isLessThan(NiveauCanonicalOrder.gradeRank("MOYENNE_SECTION"));
        assertThat(NiveauCanonicalOrder.gradeRank("MOYENNE_SECTION"))
                .isLessThan(NiveauCanonicalOrder.gradeRank("GRANDE_SECTION"));
        assertThat(NiveauCanonicalOrder.gradeRank("GRANDE_SECTION"))
                .isLessThan(NiveauCanonicalOrder.gradeRank("CP1"));
        assertThat(NiveauCanonicalOrder.gradeRank("CM2"))
                .isLessThan(NiveauCanonicalOrder.gradeRank("SIXIEME"));
        assertThat(NiveauCanonicalOrder.gradeRank("CM2"))
                .isLessThan(NiveauCanonicalOrder.gradeRank("SEPTIEME"));
        assertThat(NiveauCanonicalOrder.gradeRank("DIXIEME"))
                .isLessThan(NiveauCanonicalOrder.gradeRank("ONZIEME"));
        assertThat(NiveauCanonicalOrder.gradeRank("ONZIEME"))
                .isLessThan(NiveauCanonicalOrder.gradeRank("DOUZIEME"));
        assertThat(NiveauCanonicalOrder.gradeRank("DOUZIEME"))
                .isLessThan(NiveauCanonicalOrder.gradeRank("TERMINALE"));
    }

    @Test
    void gradeRank_unknownOrNullGrade_returnsNull() {
        assertThat(NiveauCanonicalOrder.gradeRank(null)).isNull();
        assertThat(NiveauCanonicalOrder.gradeRank("INCONNU")).isNull();
    }

    @Test
    void gradeRank_primaire_ordersCp1ThroughCm2() {
        int cp1 = NiveauCanonicalOrder.gradeRank("CP1");
        int cp2 = NiveauCanonicalOrder.gradeRank("CP2");
        int ce1 = NiveauCanonicalOrder.gradeRank("CE1");
        int cm2 = NiveauCanonicalOrder.gradeRank("CM2");

        assertThat(cp1).isLessThan(cp2);
        assertThat(cp2).isLessThan(ce1);
        assertThat(ce1).isLessThan(cm2);
    }

    @Test
    void niveauFallbackRank_ordersMaternelleBeforePrimaireBeforeSecondaire() {
        assertThat(NiveauCanonicalOrder.niveauFallbackRank("MATERNELLE", null))
                .isLessThan(NiveauCanonicalOrder.niveauFallbackRank("PRIMAIRE", null));
        assertThat(NiveauCanonicalOrder.niveauFallbackRank("PRIMAIRE", null))
                .isLessThan(NiveauCanonicalOrder.niveauFallbackRank("COLLEGE", null)); // COLLEGE = Secondaire
    }

    @Test
    void niveauFallbackRank_unknownOrNullCycle_returnsNull() {
        assertThat(NiveauCanonicalOrder.niveauFallbackRank(null, 3)).isNull();
        assertThat(NiveauCanonicalOrder.niveauFallbackRank("INCONNU", 3)).isNull();
    }

    @Test
    void niveauFallbackRank_usesSortOrderWithinSameCycle() {
        Integer bas = NiveauCanonicalOrder.niveauFallbackRank("PRIMAIRE", 1);
        Integer haut = NiveauCanonicalOrder.niveauFallbackRank("PRIMAIRE", 3);

        assertThat(bas).isLessThan(haut);
    }

    @Test
    void effectiveRank_nullLevel_returnsNull() {
        assertThat(NiveauCanonicalOrder.effectiveRank(null)).isNull();
    }

    @Test
    void effectiveRank_recognizedGradeAlwaysBeatsNiveauFallback() {
        // Un grade reconnu sur la classe doit toujours passer avant le repli grossier sur le niveau,
        // même si le sort_order du repli est très bas.
        int reconnu = NiveauCanonicalOrder.effectiveRank(new ActiveGrade("CM2", "PRIMAIRE", 99));
        int repli = NiveauCanonicalOrder.effectiveRank(new ActiveGrade(null, "PRIMAIRE", 0));

        assertThat(reconnu).isLessThan(repli);
    }

    @Test
    void effectiveRank_unrecognizedGrade_fallsBackToNiveauCycleAndSortOrder() {
        Integer rank = NiveauCanonicalOrder.effectiveRank(new ActiveGrade("GRADE_MAISON", "PRIMAIRE", 3));

        assertThat(rank).isEqualTo(NiveauCanonicalOrder.niveauFallbackRank("PRIMAIRE", 3));
    }

    @Test
    void effectiveRank_noGradeAndNoNiveau_returnsNull() {
        assertThat(NiveauCanonicalOrder.effectiveRank(new ActiveGrade(null, null, null))).isNull();
    }
}
