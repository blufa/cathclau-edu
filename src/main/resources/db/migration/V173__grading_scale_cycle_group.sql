-- Le barème (mentions/seuils) était unique par établissement (tenant_id UNIQUE). Distinction demandée
-- entre le cycle primaire (Garderie/Maternelle/Primaire) et le cycle secondaire (Collège/Lycée/
-- Supérieur) : chaque tenant peut désormais avoir un barème par groupe de cycle.
ALTER TABLE grading_scales ADD COLUMN cycle_group VARCHAR(20);

-- Le barème existant (le cas échéant) devient le barème du primaire...
UPDATE grading_scales SET cycle_group = 'PRIMAIRE' WHERE cycle_group IS NULL;

-- Retire l'unicité par tenant_id seul AVANT de dupliquer les lignes ci-dessous : sinon la ligne
-- SECONDAIRE insérée avec le même tenant_id que la ligne PRIMAIRE viole encore l'ancienne contrainte
-- (bug constaté : "duplicate key value violates unique constraint grading_scales_tenant_id_key").
DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN
        SELECT con.conname
        FROM pg_constraint con
        JOIN pg_class rel ON rel.oid = con.conrelid
        WHERE rel.relname = 'grading_scales' AND con.contype = 'u'
          AND pg_get_constraintdef(con.oid) LIKE '%tenant_id%'
          AND pg_get_constraintdef(con.oid) NOT LIKE '%cycle_group%'
    LOOP
        EXECUTE format('ALTER TABLE grading_scales DROP CONSTRAINT %I', r.conname);
    END LOOP;
END $$;

-- ...et est dupliqué tel quel comme point de départ du barème du secondaire, à ajuster ensuite par
-- l'établissement si besoin.
INSERT INTO grading_scales (tenant_id, cycle_group, pass_threshold, passable_min, assez_bien_min,
                             bien_min, tres_bien_min, felicitations_min, compliments_min,
                             distinction_no_failing_threshold)
SELECT tenant_id, 'SECONDAIRE', pass_threshold, passable_min, assez_bien_min, bien_min, tres_bien_min,
       felicitations_min, compliments_min, distinction_no_failing_threshold
FROM grading_scales
WHERE cycle_group = 'PRIMAIRE';

ALTER TABLE grading_scales ALTER COLUMN cycle_group SET NOT NULL;

ALTER TABLE grading_scales ADD CONSTRAINT uq_grading_scales_tenant_cycle UNIQUE (tenant_id, cycle_group);
