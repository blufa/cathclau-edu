-- Réglages école pour le calcul des heures réalisées enseignant (§heures) : marge de transition entre
-- deux séances consécutives dans des salles différentes, et fenêtres de récréation quotidiennes
-- (temps défini par l'école, non imputable à l'enseignant).
CREATE TABLE IF NOT EXISTS attendance_settings (
    id                        BIGSERIAL PRIMARY KEY,
    tenant_id                 BIGINT NOT NULL,
    transition_margin_minutes INTEGER NOT NULL DEFAULT 5,
    CONSTRAINT uq_attendance_settings_tenant UNIQUE (tenant_id)
);

CREATE TABLE IF NOT EXISTS attendance_recess_windows (
    id         BIGSERIAL PRIMARY KEY,
    tenant_id  BIGINT NOT NULL,
    label      VARCHAR(100),
    start_time TIME NOT NULL,
    end_time   TIME NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_attendance_recess_windows_tenant ON attendance_recess_windows (tenant_id);

-- Départ anticipé de séance (symétrique de late_minutes) : sessionEnd − checkOutTime quand la sortie
-- est pointée avant la fin prévue. Absent de pointage de sortie => pas de pénalité (bénéfice du doute).
ALTER TABLE teacher_checkins ADD COLUMN IF NOT EXISTS early_minutes INTEGER NOT NULL DEFAULT 0;
