package gn.lacaeducation.academics.domain.valueobject;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GradeTest {

    @Test
    void mepuaCycle_maps_each_grade_to_its_official_cycle() {
        assertThat(Grade.GARDERIE.mepuaCycle()).isEqualTo(SchoolCycle.GARDERIE);
        assertThat(Grade.PETITE_SECTION.mepuaCycle()).isEqualTo(SchoolCycle.MATERNELLE);
        assertThat(Grade.MOYENNE_SECTION.mepuaCycle()).isEqualTo(SchoolCycle.MATERNELLE);
        assertThat(Grade.GRANDE_SECTION.mepuaCycle()).isEqualTo(SchoolCycle.MATERNELLE);
        assertThat(Grade.CP1.mepuaCycle()).isEqualTo(SchoolCycle.PRIMAIRE);
        assertThat(Grade.CM2.mepuaCycle()).isEqualTo(SchoolCycle.PRIMAIRE);
        assertThat(Grade.SIXIEME.mepuaCycle()).isEqualTo(SchoolCycle.COLLEGE);
        assertThat(Grade.SEPTIEME.mepuaCycle()).isEqualTo(SchoolCycle.COLLEGE);
        assertThat(Grade.DIXIEME.mepuaCycle()).isEqualTo(SchoolCycle.COLLEGE);
        assertThat(Grade.SECONDE.mepuaCycle()).isEqualTo(SchoolCycle.LYCEE);
        assertThat(Grade.ONZIEME.mepuaCycle()).isEqualTo(SchoolCycle.LYCEE);
        assertThat(Grade.TERMINALE.mepuaCycle()).isEqualTo(SchoolCycle.LYCEE);
    }

    @Test
    void declaration_order_is_the_canonical_pedagogical_progression() {
        assertThat(Grade.CP1.ordinal()).isLessThan(Grade.CP2.ordinal());
        assertThat(Grade.CM2.ordinal()).isLessThan(Grade.SIXIEME.ordinal());
        assertThat(Grade.CM2.ordinal()).isLessThan(Grade.SEPTIEME.ordinal());
        assertThat(Grade.DIXIEME.ordinal()).isLessThan(Grade.ONZIEME.ordinal());
        assertThat(Grade.ONZIEME.ordinal()).isLessThan(Grade.DOUZIEME.ordinal());
        assertThat(Grade.DOUZIEME.ordinal()).isLessThan(Grade.TERMINALE.ordinal());
    }
}
