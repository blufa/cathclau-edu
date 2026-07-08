-- Personnel non-enseignant (administratif, direction, soutien) — §7.1. Tenant-scopé.
-- Les enseignants restent dans la table `teachers` ; cette table couvre le reste du personnel salarié.
CREATE TABLE employees (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL,
    user_id         BIGINT,
    employee_number VARCHAR(64),
    first_name      VARCHAR(128),
    last_name       VARCHAR(128),
    phone           VARCHAR(32),
    email           VARCHAR(255),
    category        VARCHAR(16)  NOT NULL,
    job_title       VARCHAR(128),
    hire_date       DATE,
    base_salary     NUMERIC(15, 2),
    cnss_number     VARCHAR(64),
    active          BOOLEAN      NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_employees_tenant ON employees (tenant_id);
CREATE INDEX idx_employees_category ON employees (tenant_id, category);
CREATE UNIQUE INDEX uq_employees_tenant_number ON employees (tenant_id, employee_number)
    WHERE employee_number IS NOT NULL;
