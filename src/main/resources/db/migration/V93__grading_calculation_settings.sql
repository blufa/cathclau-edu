-- Réglage par établissement du moteur de calcul des moyennes (§2 + §5) : méthode, décimales, arrondi.
-- En l'absence de ligne : moyenne pondérée, 2 décimales, arrondi mathématique (comportement historique).
CREATE TABLE grading_calculation_settings (
    id             BIGSERIAL PRIMARY KEY,
    tenant_id      BIGINT      NOT NULL UNIQUE,
    average_method VARCHAR(20) NOT NULL DEFAULT 'WEIGHTED',
    decimals       INTEGER     NOT NULL DEFAULT 2,
    rounding       VARCHAR(20) NOT NULL DEFAULT 'MATHEMATICAL'
);
