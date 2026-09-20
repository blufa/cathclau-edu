package gn.lacaeducation.billing.application.service;

import gn.lacaeducation.billing.application.port.out.EnrollmentQueryPort.ActiveLevel;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NiveauCanonicalOrderTest {

    @Test
    void cycleRank_ordersGarderieBeforeMaternelleBeforePrimaireBeforeCollegeBeforeLyceeBeforeSuperieur() {
        assertThat(NiveauCanonicalOrder.cycleRank("GARDERIE"))
                .isLessThan(NiveauCanonicalOrder.cycleRank("MATERNELLE"));
        assertThat(NiveauCanonicalOrder.cycleRank("MATERNELLE"))
                .isLessThan(NiveauCanonicalOrder.cycleRank("PRIMAIRE"));
        assertThat(NiveauCanonicalOrder.cycleRank("PRIMAIRE"))
                .isLessThan(NiveauCanonicalOrder.cycleRank("COLLEGE"));
        assertThat(NiveauCanonicalOrder.cycleRank("COLLEGE"))
                .isLessThan(NiveauCanonicalOrder.cycleRank("LYCEE"));
        assertThat(NiveauCanonicalOrder.cycleRank("LYCEE"))
                .isLessThan(NiveauCanonicalOrder.cycleRank("SUPERIEUR"));
    }

    @Test
    void cycleRank_unknownOrNullCycle_returnsNull() {
        assertThat(NiveauCanonicalOrder.cycleRank(null)).isNull();
        assertThat(NiveauCanonicalOrder.cycleRank("INCONNU")).isNull();
    }

    @Test
    void effectiveWithinCycleRank_maternelleSections_orderedPetiteThenMoyenneThenGrande() {
        int petite = NiveauCanonicalOrder.effectiveWithinCycleRank(new ActiveLevel("MATERNELLE", "Petite Section", null));
        int moyenne = NiveauCanonicalOrder.effectiveWithinCycleRank(new ActiveLevel("MATERNELLE", "Moyenne Section", null));
        int grande = NiveauCanonicalOrder.effectiveWithinCycleRank(new ActiveLevel("MATERNELLE", "Grande Section", null));

        assertThat(petite).isLessThan(moyenne);
        assertThat(moyenne).isLessThan(grande);
    }

    @Test
    void effectiveWithinCycleRank_maternelleSections_insensitiveToAccentsCaseAndSortOrder() {
        // Même si l'école a saisi un sort_order incohérent (Grande Section=1, avant Moyenne
        // Section=2), le libellé canonique doit toujours l'emporter.
        int grandeAvecSortOrderBas = NiveauCanonicalOrder.effectiveWithinCycleRank(new ActiveLevel("MATERNELLE", "grande section", 1));
        int moyenneAvecSortOrderHaut = NiveauCanonicalOrder.effectiveWithinCycleRank(new ActiveLevel("MATERNELLE", "MOYENNE SECTION", 2));

        assertThat(moyenneAvecSortOrderHaut).isLessThan(grandeAvecSortOrderBas);
    }

    @Test
    void effectiveWithinCycleRank_primaire_ordersCp1ThroughCm2() {
        int cp1 = NiveauCanonicalOrder.effectiveWithinCycleRank(new ActiveLevel("PRIMAIRE", "CP1", null));
        int cp2 = NiveauCanonicalOrder.effectiveWithinCycleRank(new ActiveLevel("PRIMAIRE", "CP2", null));
        int ce1 = NiveauCanonicalOrder.effectiveWithinCycleRank(new ActiveLevel("PRIMAIRE", "CE1", null));
        int cm2 = NiveauCanonicalOrder.effectiveWithinCycleRank(new ActiveLevel("PRIMAIRE", "CM2", null));

        assertThat(cp1).isLessThan(cp2);
        assertThat(cp2).isLessThan(ce1);
        assertThat(ce1).isLessThan(cm2);
    }

    @Test
    void effectiveWithinCycleRank_lycee_terminaleIsLast() {
        int onzieme = NiveauCanonicalOrder.effectiveWithinCycleRank(new ActiveLevel("LYCEE", "11ème", null));
        int douzieme = NiveauCanonicalOrder.effectiveWithinCycleRank(new ActiveLevel("LYCEE", "12ème", null));
        int terminale = NiveauCanonicalOrder.effectiveWithinCycleRank(new ActiveLevel("LYCEE", "Terminale", null));

        assertThat(onzieme).isLessThan(douzieme);
        assertThat(douzieme).isLessThan(terminale);
    }

    @Test
    void effectiveWithinCycleRank_unrecognizedLabel_fallsBackToSortOrderOffsetBy1000() {
        Integer rank = NiveauCanonicalOrder.effectiveWithinCycleRank(new ActiveLevel("PRIMAIRE", "Niveau maison", 3));

        assertThat(rank).isEqualTo(1003);
    }

    @Test
    void effectiveWithinCycleRank_unrecognizedLabelAndNoSortOrder_returnsNull() {
        assertThat(NiveauCanonicalOrder.effectiveWithinCycleRank(new ActiveLevel("PRIMAIRE", "Niveau maison", null))).isNull();
    }

    @Test
    void effectiveWithinCycleRank_nullLevel_returnsNull() {
        assertThat(NiveauCanonicalOrder.effectiveWithinCycleRank(null)).isNull();
    }

    @Test
    void effectiveWithinCycleRank_recognizedLabelAlwaysBeatsUnrecognizedFallback() {
        // Un niveau reconnu (rang 0..n) doit toujours passer avant un niveau non reconnu (replié sur
        // 1000+sortOrder), même si le sort_order du niveau non reconnu est très petit.
        int reconnu = NiveauCanonicalOrder.effectiveWithinCycleRank(new ActiveLevel("PRIMAIRE", "CM2", 99));
        int nonReconnu = NiveauCanonicalOrder.effectiveWithinCycleRank(new ActiveLevel("PRIMAIRE", "Niveau maison", 0));

        assertThat(reconnu).isLessThan(nonReconnu);
    }
}
