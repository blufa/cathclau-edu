-- Avances sur salaire (§payroll) : montant accordé au personnel/enseignant, plafonné à son salaire de
-- référence, remboursé automatiquement par retenue sur les fiches de paie suivantes jusqu'à épuisement
-- du solde (voir SalaryAdvanceService#applyDeduction, appelée depuis PayrollService#generate).

CREATE TABLE salary_advances (
    id                BIGSERIAL PRIMARY KEY,
    tenant_id         BIGINT         NOT NULL,
    employee_type     VARCHAR(16)    NOT NULL,
    teacher_id        BIGINT         NOT NULL,
    teacher_name      VARCHAR(255),
    amount            NUMERIC(15, 2) NOT NULL,
    remaining_balance NUMERIC(15, 2) NOT NULL,
    reason            TEXT,
    status            VARCHAR(16)    NOT NULL DEFAULT 'ACTIVE',
    granted_at        TIMESTAMP,
    last_deducted_at  TIMESTAMP
);
CREATE INDEX idx_salary_advances_tenant ON salary_advances (tenant_id);
CREATE INDEX idx_salary_advances_teacher ON salary_advances (tenant_id, teacher_id, employee_type);
