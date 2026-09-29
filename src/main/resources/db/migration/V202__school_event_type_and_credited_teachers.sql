-- Jour férié (chômé et payé, décidé par l'État) : toujours payé automatiquement sur les heures
-- touchées. Autre événement école (kermesse, sortie...) : la direction choisit explicitement, parmi
-- les enseignants dont un créneau est touché, lesquels sont considérés comme ayant fait cours
-- (§payroll ↔ academics — PayrollService.eventCoveredMinutesByClass).

ALTER TABLE school_events ADD COLUMN type VARCHAR(20) NOT NULL DEFAULT 'OTHER';

CREATE TABLE school_event_credited_teachers (
    id              BIGSERIAL PRIMARY KEY,
    school_event_id BIGINT NOT NULL REFERENCES school_events(id) ON DELETE CASCADE,
    teacher_id      BIGINT NOT NULL,
    UNIQUE (school_event_id, teacher_id)
);
CREATE INDEX idx_school_event_credited_teachers_event ON school_event_credited_teachers (school_event_id);
