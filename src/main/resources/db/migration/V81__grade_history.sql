-- Historique des modifications de notes : trace chaque changement (saisie, correction,
-- validation, déverrouillage, suppression), avec l'ancienne/nouvelle valeur, l'auteur et
-- la raison éventuelle. Append-only : permet de comprendre "pourquoi tant de modifications".
CREATE TABLE grade_history (
    id BIGSERIAL PRIMARY KEY,
    grade_id BIGINT,
    assessment_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    action VARCHAR(20) NOT NULL CHECK (action IN ('ENTERED','UPDATED','VALIDATED','UNLOCKED','DELETED')),
    old_value NUMERIC(5,2),
    new_value NUMERIC(5,2),
    changed_by BIGINT,
    reason VARCHAR(500),
    changed_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_grade_history_grade ON grade_history(grade_id);
CREATE INDEX idx_grade_history_assessment ON grade_history(assessment_id);
CREATE INDEX idx_grade_history_student ON grade_history(student_id);
