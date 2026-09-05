package gn.lacaeducation.academics.domain.valueobject;

/**
 * Cycle d'enseignement auquel se rattache une période. Détermine le découpage de l'année :
 * le primaire fonctionne en trimestres, le collège/lycée/supérieur en semestres.
 */
public enum SchoolCycle {
    /** Garderie : accueil de la petite enfance, en dehors de la progression pédagogique
     * Petite/Moyenne/Grande Section de la maternelle — distinct de MATERNELLE mais soumis aux mêmes
     * règles de sécurité (badge de retrait obligatoire à la sortie). */
    GARDERIE, MATERNELLE, PRIMAIRE, COLLEGE, LYCEE, SUPERIEUR;

    /** Type de période par défaut pour ce cycle (TRIMESTRE en garderie/maternelle/primaire, SEMESTRE au-delà). */
    public TypePeriode defaultPeriodType() {
        return this == GARDERIE || this == MATERNELLE || this == PRIMAIRE ? TypePeriode.TRIMESTRE : TypePeriode.SEMESTRE;
    }
}
