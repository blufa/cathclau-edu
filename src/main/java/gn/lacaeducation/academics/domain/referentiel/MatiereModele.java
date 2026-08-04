package gn.lacaeducation.academics.domain.referentiel;

import java.math.BigDecimal;

/** Entrée du catalogue de référence MEPUA : matière + coefficient officiel pour un cycle/série donné. */
public record MatiereModele(String code, String libelle, BigDecimal coefficient) {
}
