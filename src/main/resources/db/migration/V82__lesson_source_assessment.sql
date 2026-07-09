-- Une évaluation créée par un enseignant est comptabilisée comme une séance d'enseignement
-- (au même titre qu'une leçon) dans ses heures travaillées. On trace l'évaluation d'origine
-- pour garantir l'idempotence (une seule séance générée par évaluation).
ALTER TABLE lesson_entries ADD COLUMN source_assessment_id BIGINT;

CREATE UNIQUE INDEX uq_lessons_source_assessment
    ON lesson_entries (tenant_id, source_assessment_id)
    WHERE source_assessment_id IS NOT NULL;
