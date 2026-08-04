-- Formules de calcul de moyenne paramétrables (remplace le calcul plat codé en dur).
-- IF NOT EXISTS partout : ddl-auto=update (Hibernate) peut avoir déjà créé ces tables depuis les
-- entités JPA avant que Flyway ne s'exécute sur un environnement de dev déjà démarré une fois.
CREATE TABLE IF NOT EXISTS grading_formula (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    label VARCHAR(200) NOT NULL,
    period_type VARCHAR(30) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE INDEX IF NOT EXISTS idx_grading_formula_tenant ON grading_formula(tenant_id);
-- Une seule formule active par tenant et par type de période (résolution sans ambiguïté par le moteur).
CREATE UNIQUE INDEX IF NOT EXISTS uq_grading_formula_active_per_period_type
    ON grading_formula(tenant_id, period_type) WHERE active = TRUE;

CREATE TABLE IF NOT EXISTS grading_formula_step (
    id BIGSERIAL PRIMARY KEY,
    formula_id BIGINT NOT NULL REFERENCES grading_formula(id),
    step_order INTEGER NOT NULL,
    label VARCHAR(200),
    step_kind VARCHAR(20) NOT NULL CHECK (step_kind IN ('GROUP','COMBINE')),
    month_index INTEGER,
    assessment_type_id BIGINT,
    is_final BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE (formula_id, step_order)
);
CREATE INDEX IF NOT EXISTS idx_grading_formula_step_formula ON grading_formula_step(formula_id);

CREATE TABLE IF NOT EXISTS grading_formula_step_input (
    id BIGSERIAL PRIMARY KEY,
    step_id BIGINT NOT NULL REFERENCES grading_formula_step(id),
    ref_step_order INTEGER NOT NULL,
    weight_mode VARCHAR(20) NOT NULL CHECK (weight_mode IN ('EQUAL','SUBJECT_COEFFICIENT','FIXED')),
    fixed_weight NUMERIC(8,2)
);
CREATE INDEX IF NOT EXISTS idx_grading_formula_step_input_step ON grading_formula_step_input(step_id);
