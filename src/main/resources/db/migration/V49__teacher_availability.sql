-- Disponibilités déclarées par chaque enseignant (jour + plage horaire). L'emploi du temps s'appuie
-- dessus : on ne propose qu'un créneau dans une plage disponible, et une heure prise devient indisponible.
CREATE TABLE teacher_availability (
    id         BIGSERIAL PRIMARY KEY,
    tenant_id  BIGINT      NOT NULL,
    teacher_id BIGINT      NOT NULL,
    day        VARCHAR(15) NOT NULL,
    start_time TIME        NOT NULL,
    end_time   TIME        NOT NULL
);
CREATE INDEX idx_teacher_availability_teacher ON teacher_availability (teacher_id);
