package gn.lacaeducation.academics.domain.valueobject;

/**
 * Cycle d'enseignement auquel se rattache une période. Détermine le découpage de l'année :
 * le primaire fonctionne en trimestres, le collège/lycée/supérieur en semestres.
 */
public enum SchoolCycle {
    MATERNELLE, PRIMAIRE, COLLEGE, LYCEE, SUPERIEUR;

    /** Type de période par défaut pour ce cycle (TRIMESTRE en maternelle/primaire, SEMESTRE au-delà). */
    public TypePeriode defaultPeriodType() {
        return this == MATERNELLE || this == PRIMAIRE ? TypePeriode.TRIMESTRE : TypePeriode.SEMESTRE;
    }
}
