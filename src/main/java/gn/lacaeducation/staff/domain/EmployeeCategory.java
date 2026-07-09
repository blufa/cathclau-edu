package gn.lacaeducation.staff.domain;

/**
 * Catégorie de personnel non-enseignant (§7.1). Les enseignants restent modélisés par {@link Teacher} ;
 * cette énumération couvre le reste du personnel salarié de l'établissement.
 */
public enum EmployeeCategory {
    /** Personnel administratif (secrétariat, comptabilité, surveillance…). */
    ADMINISTRATIVE,
    /** Équipe de direction (directeur, censeur, proviseur…). */
    DIRECTION,
    /** Personnel de soutien/logistique (entretien, gardiennage, cantine…). */
    SUPPORT
}
