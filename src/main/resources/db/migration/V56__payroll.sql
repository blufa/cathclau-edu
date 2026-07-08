-- Module Paie (Payroll) : fiches de paie des enseignants/personnel (inspiré OpenEduCat).
-- Tenant-scopé. S'appuie sur les rémunérations saisies sur la fiche enseignant (par identifiant) ;
-- les primes/retenues sont des lignes ; brut/net sont recalculés à chaque modification.

CREATE TABLE payslips (
    id             BIGSERIAL PRIMARY KEY,
    tenant_id      BIGINT        NOT NULL,
    teacher_id     BIGINT        NOT NULL,
    period_label   VARCHAR(64)   NOT NULL,
    period_start   DATE,
    period_end     DATE,
    base_salary    NUMERIC(15, 2) NOT NULL DEFAULT 0,
    gross_salary   NUMERIC(15, 2) NOT NULL DEFAULT 0,
    net_salary     NUMERIC(15, 2) NOT NULL DEFAULT 0,
    status         VARCHAR(16)   NOT NULL DEFAULT 'DRAFT',
    payment_method VARCHAR(24),
    notes          TEXT,
    created_at     TIMESTAMP,
    paid_at        TIMESTAMP
);
CREATE INDEX idx_payslips_tenant ON payslips (tenant_id);
CREATE INDEX idx_payslips_teacher ON payslips (teacher_id);

CREATE TABLE payslip_lines (
    id         BIGSERIAL PRIMARY KEY,
    tenant_id  BIGINT         NOT NULL,
    payslip_id BIGINT         NOT NULL REFERENCES payslips (id) ON DELETE CASCADE,
    label      VARCHAR(255)   NOT NULL,
    line_type  VARCHAR(16)    NOT NULL,
    amount     NUMERIC(15, 2) NOT NULL DEFAULT 0
);
CREATE INDEX idx_payslip_lines_payslip ON payslip_lines (payslip_id);
CREATE INDEX idx_payslip_lines_tenant ON payslip_lines (tenant_id);

-- Configuration : structures salariales et leurs règles (primes/retenues, % ou montant fixe).
CREATE TABLE salary_structures (
    id        BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT       NOT NULL,
    name      VARCHAR(128) NOT NULL,
    code      VARCHAR(32),
    active    BOOLEAN      NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_salary_structures_tenant ON salary_structures (tenant_id);

CREATE TABLE salary_rules (
    id           BIGSERIAL PRIMARY KEY,
    tenant_id    BIGINT         NOT NULL,
    structure_id BIGINT         NOT NULL REFERENCES salary_structures (id) ON DELETE CASCADE,
    label        VARCHAR(255)   NOT NULL,
    code         VARCHAR(32),
    category     VARCHAR(32),
    line_type    VARCHAR(16)    NOT NULL,
    computation  VARCHAR(24)    NOT NULL,
    value        NUMERIC(15, 4) NOT NULL DEFAULT 0,
    sequence     INTEGER        NOT NULL DEFAULT 10
);
CREATE INDEX idx_salary_rules_structure ON salary_rules (structure_id);
CREATE INDEX idx_salary_rules_tenant ON salary_rules (tenant_id);
