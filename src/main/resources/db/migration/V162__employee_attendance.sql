-- Présence du personnel non-enseignant (administratif, direction, soutien — §staff.Employee), miroir
-- simplifié de teacher_attendances : un seul pointage par jour (pas de créneau de cours comme les
-- enseignants), retard/départ anticipé calculés vs les horaires configurés de l'école.
CREATE TABLE IF NOT EXISTS employee_attendances (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    employee_id     BIGINT NOT NULL,
    date            DATE NOT NULL,
    status          VARCHAR(20) NOT NULL CHECK (status IN ('PRESENT','ABSENT')),
    reason          VARCHAR(500),
    check_in_time   TIME,
    check_out_time  TIME,
    late_minutes    INTEGER NOT NULL DEFAULT 0,
    early_minutes   INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uq_employee_attendance UNIQUE (employee_id, date)
);
CREATE INDEX IF NOT EXISTS idx_employee_attendances_employee ON employee_attendances (employee_id);
CREATE INDEX IF NOT EXISTS idx_employee_attendances_tenant_date ON employee_attendances (tenant_id, date);

-- Horaires de référence du personnel non-enseignant (un seul horaire pour tout le personnel, pas par
-- individu, comme le reste des réglages de présence de l'école).
ALTER TABLE attendance_settings ADD COLUMN IF NOT EXISTS employee_check_in_time  TIME NOT NULL DEFAULT '08:00';
ALTER TABLE attendance_settings ADD COLUMN IF NOT EXISTS employee_check_out_time TIME NOT NULL DEFAULT '17:00';
