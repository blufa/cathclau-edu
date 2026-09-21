package gn.lacaeducation.academics.domain.referentiel;

import gn.lacaeducation.academics.domain.valueobject.Grade;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogueMepuaTest {

    @Test
    void pourClasse_returns_empty_when_grade_is_null() {
        assertThat(CatalogueMepua.pourClasse(null, null)).isEmpty();
    }

    @Test
    void pourClasse_college_grade_resolves_college_catalogue_regardless_of_nomenclature() {
        assertThat(CatalogueMepua.pourClasse(Grade.SEPTIEME, null)).isEqualTo(CatalogueMepua.COLLEGE);
        assertThat(CatalogueMepua.pourClasse(Grade.SIXIEME, null)).isEqualTo(CatalogueMepua.COLLEGE);
    }

    @Test
    void pourClasse_lycee_grade_dispatches_on_serie() {
        assertThat(CatalogueMepua.pourClasse(Grade.TERMINALE, "LITTERAIRE")).isEqualTo(CatalogueMepua.LITTERAIRE);
        assertThat(CatalogueMepua.pourClasse(Grade.ONZIEME, "SCIENTIFIQUE")).isEqualTo(CatalogueMepua.SCIENTIFIQUE);
        assertThat(CatalogueMepua.pourClasse(Grade.TERMINALE, null)).isEmpty();
    }

    @Test
    void pourClasse_primaire_grade_resolves_primaire_catalogue() {
        assertThat(CatalogueMepua.pourClasse(Grade.CP1, null)).isEqualTo(CatalogueMepua.PRIMAIRE);
    }

    @Test
    void pourClasse_maternelle_grade_has_no_official_catalogue() {
        assertThat(CatalogueMepua.pourClasse(Grade.GRANDE_SECTION, null)).isEmpty();
    }
}
