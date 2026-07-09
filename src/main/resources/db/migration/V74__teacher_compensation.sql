-- Rémunération du personnel enseignant (module Paie) : mode horaire (payé aux heures effectives) ou
-- fixe (salaire mensuel) ; un membre de la direction en salaire fixe peut aussi enseigner (ses heures
-- de cours s'ajoutent au tarif horaire).
CREATE TABLE IF NOT EXISTS teacher_compensation (
    id           BIGSERIAL PRIMARY KEY,
    tenant_id    BIGINT NOT NULL,
    teacher_id   BIGINT NOT NULL,
    pay_mode     VARCHAR(16) NOT NULL DEFAULT 'HOURLY',
    hourly_rate  NUMERIC(15, 2) NOT NULL DEFAULT 0,
    fixed_salary NUMERIC(15, 2) NOT NULL DEFAULT 0,
    CONSTRAINT uq_teacher_compensation UNIQUE (teacher_id)
);
CREATE INDEX IF NOT EXISTS idx_teacher_compensation_tenant ON teacher_compensation (tenant_id);
