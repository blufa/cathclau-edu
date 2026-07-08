-- Lien optionnel d'une évaluation vers un type d'évaluation configurable (§3).
ALTER TABLE assessments ADD COLUMN assessment_type_id BIGINT;
CREATE INDEX idx_assessments_type_config ON assessments (assessment_type_id);
