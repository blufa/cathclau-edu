-- Remplace l'avantage "payé pendant les vacances" (interrupteur oui/non valable toute l'année) par
-- un choix mois par mois précis (§paie) : l'école décide, au cas par cas, quels mois de vacances
-- précis d'une année précise sont payés comme si l'enseignant avait travaillé.
CREATE TABLE IF NOT EXISTS teacher_vacation_pay_months (
    id         BIGSERIAL PRIMARY KEY,
    tenant_id  BIGINT NOT NULL,
    teacher_id BIGINT NOT NULL,
    year       INTEGER NOT NULL,
    month      INTEGER NOT NULL CHECK (month BETWEEN 1 AND 12),
    CONSTRAINT uq_teacher_vacation_pay_month UNIQUE (teacher_id, year, month)
);
CREATE INDEX IF NOT EXISTS idx_teacher_vacation_pay_teacher ON teacher_vacation_pay_months (teacher_id);

ALTER TABLE teacher_compensation DROP COLUMN IF EXISTS paid_during_vacation;
