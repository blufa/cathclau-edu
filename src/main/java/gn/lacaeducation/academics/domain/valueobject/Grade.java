package gn.lacaeducation.academics.domain.valueobject;

/**
 * Grade pédagogique précis d'une classe (ex. CP1, SEPTIEME, TERMINALE), indépendant du Niveau (qui ne
 * porte plus que le regroupement grossier Maternelle/Primaire/Secondaire depuis la réforme visant à
 * simplifier le catalogue de référentiels pour un personnel non technique). L'ordre de déclaration EST
 * l'ordre canonique de progression garderie → terminale ; ne pas réordonner les constantes sans mettre
 * à jour {@code NiveauCanonicalOrder} (module billing) qui s'appuie sur {@code ordinal()}.
 */
public enum Grade {
    GARDERIE,
    PETITE_SECTION, MOYENNE_SECTION, GRANDE_SECTION,
    CP1, CP2, CE1, CE2, CM1, CM2,
    SIXIEME, CINQUIEME, QUATRIEME, TROISIEME,
    SEPTIEME, HUITIEME, NEUVIEME, DIXIEME,
    SECONDE, PREMIERE,
    ONZIEME, DOUZIEME, TERMINALE;

    /**
     * Cycle MEPUA du grade — seule source encore utilisée pour distinguer Collège de Lycée après la
     * réforme du Niveau (voir {@code CatalogueMepua.pourClasse()}), les grilles de matières/coefficients
     * officielles restant différentes entre les deux.
     */
    public SchoolCycle mepuaCycle() {
        return switch (this) {
            case GARDERIE -> SchoolCycle.GARDERIE;
            case PETITE_SECTION, MOYENNE_SECTION, GRANDE_SECTION -> SchoolCycle.MATERNELLE;
            case CP1, CP2, CE1, CE2, CM1, CM2 -> SchoolCycle.PRIMAIRE;
            case SIXIEME, CINQUIEME, QUATRIEME, TROISIEME,
                 SEPTIEME, HUITIEME, NEUVIEME, DIXIEME -> SchoolCycle.COLLEGE;
            case SECONDE, PREMIERE, ONZIEME, DOUZIEME, TERMINALE -> SchoolCycle.LYCEE;
        };
    }
}
