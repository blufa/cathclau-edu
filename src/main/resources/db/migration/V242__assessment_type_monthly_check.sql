-- Bug réel découvert en test réel : AssessmentType.MONTHLY existe côté Java (grading/domain) depuis
-- V239 mais la contrainte CHECK posée par V8 sur assessments.type n'avait jamais été élargie —
-- toute création d'évaluation mensuelle échouait en base (violation CHECK, remontée en 409 générique
-- "Conflit de données... référence invalide").
ALTER TABLE assessments DROP CONSTRAINT assessments_type_check;
ALTER TABLE assessments ADD CONSTRAINT assessments_type_check
    CHECK (type IN ('QUIZ', 'HOMEWORK', 'EXAM', 'FINAL', 'MONTHLY'));
