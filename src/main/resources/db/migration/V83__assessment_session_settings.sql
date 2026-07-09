-- Paramétrage par établissement de la durée (minutes) des séances d'enseignement générées à la
-- création d'une évaluation, par type. Ces séances sont comptabilisées dans les heures travaillées
-- de l'enseignant. En l'absence de ligne, on applique les durées par défaut du code (QUIZ 30,
-- HOMEWORK 60, EXAM 120, FINAL 180).
CREATE TABLE assessment_session_settings (
    id               BIGSERIAL PRIMARY KEY,
    tenant_id        BIGINT  NOT NULL UNIQUE,
    quiz_minutes     INTEGER NOT NULL DEFAULT 30,
    homework_minutes INTEGER NOT NULL DEFAULT 60,
    exam_minutes     INTEGER NOT NULL DEFAULT 120,
    final_minutes    INTEGER NOT NULL DEFAULT 180
);
