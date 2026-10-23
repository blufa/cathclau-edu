-- Lien optionnel d'une évaluation vers la composition (Exam) qui l'a fait naître (§retour utilisateur :
-- la saisie des notes de composition se fait désormais depuis l'onglet Compositions, matière par
-- matière — chaque matière y crée/retrouve un Assessment de type EXAM lié à l'Exam).
ALTER TABLE assessments ADD COLUMN exam_id BIGINT;
CREATE INDEX idx_assessments_exam ON assessments (exam_id);
