package gn.lacaeducation.academics.domain.valueobject;

/**
 * Cycle d'enseignement auquel se rattache une période. Détermine le découpage de l'année :
 * le primaire fonctionne en trimestres, le collège/lycée/supérieur en semestres.
 */
public enum SchoolCycle {
    PRIMAIRE, COLLEGE, LYCEE, SUPERIEUR;

    /** Type de période par défaut pour ce cycle (TRIMESTRE au primaire, SEMESTRE au-delà). */
    public TypePeriode defaultPeriodType() {
        return this == PRIMAIRE ? TypePeriode.TRIMESTRE : TypePeriode.SEMESTRE;
    }
}
