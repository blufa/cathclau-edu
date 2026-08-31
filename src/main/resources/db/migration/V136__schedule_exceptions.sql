-- Exceptions ponctuelles à l'emploi du temps récurrent : déplacement (MOVED) ou annulation
-- (CANCELLED) d'un créneau pour UNE semaine précise seulement (identifiée par son lundi), sans
-- jamais modifier le créneau récurrent (table schedules, V6) ni affecter les autres semaines.
CREATE TABLE schedule_exceptions (
    id                    BIGSERIAL PRIMARY KEY,
    tenant_id             BIGINT      NOT NULL,
    original_schedule_id  BIGINT      NOT NULL REFERENCES schedules (id) ON DELETE CASCADE,
    week_start_date       DATE        NOT NULL,
    type                  VARCHAR(15) NOT NULL,
    new_day               VARCHAR(15),
    new_start_time        TIME,
    new_end_time          TIME,
    new_room_id           BIGINT      REFERENCES rooms (id),
    new_teacher_id        BIGINT,
    reason                VARCHAR(255),
    CONSTRAINT uq_schedule_exceptions_slot_week UNIQUE (original_schedule_id, week_start_date)
);

CREATE INDEX idx_schedule_exceptions_original ON schedule_exceptions (original_schedule_id);
CREATE INDEX idx_schedule_exceptions_week ON schedule_exceptions (tenant_id, week_start_date);
