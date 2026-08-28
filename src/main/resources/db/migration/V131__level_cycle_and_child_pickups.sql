-- Cycle scolaire du niveau (maternelle/primaire/collège/lycée/supérieur) : détermine notamment si un
-- élève de cette classe déclenche le badge de retrait automatique à la sortie (§attendance).
ALTER TABLE levels ADD COLUMN IF NOT EXISTS cycle VARCHAR(20);

-- Retrait d'un enfant (maternelle/primaire) : qui est venu le récupérer, pour la preuve imprimable
-- (badge de retrait) et l'historique.
CREATE TABLE IF NOT EXISTS child_pickups (
    id                BIGSERIAL PRIMARY KEY,
    tenant_id         BIGINT NOT NULL,
    student_id        BIGINT NOT NULL,
    class_id          BIGINT NOT NULL,
    pickup_date       DATE NOT NULL,
    pickup_time       TIME NOT NULL,
    picker_parent_id  BIGINT,
    picker_name       VARCHAR(255) NOT NULL,
    picker_relation   VARCHAR(100),
    created_at        TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_child_pickups_student_date ON child_pickups (student_id, pickup_date);
