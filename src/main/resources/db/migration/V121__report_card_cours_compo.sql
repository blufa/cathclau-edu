-- Trace "cours" et "composition" par matière (méthode de calcul officielle : la composition compte
-- double dans la moyenne de matière, et la moyenne générale d'un élève combine une moyenne de cours
-- simple entre matières avec une moyenne de composition pondérée par coefficient). Les étapes de
-- formule (grading_formula_step) peuvent être étiquetées pour indiquer laquelle alimente chaque
-- colonne ; sans étiquetage (role='NONE' par défaut), le calcul historique reste inchangé.
ALTER TABLE report_card_lines ADD COLUMN IF NOT EXISTS cours_average NUMERIC(5,2);
ALTER TABLE report_card_lines ADD COLUMN IF NOT EXISTS compo_average NUMERIC(5,2);

ALTER TABLE grading_formula_step ADD COLUMN IF NOT EXISTS role VARCHAR(20) NOT NULL DEFAULT 'NONE'
    CHECK (role IN ('NONE','COURS','COMPOSITION'));

-- Poids de la moyenne de cours / composition dans la moyenne générale (défaut 1 et 2 si non renseigné).
ALTER TABLE grading_calculation_settings ADD COLUMN IF NOT EXISTS general_cours_weight NUMERIC(5,2);
ALTER TABLE grading_calculation_settings ADD COLUMN IF NOT EXISTS general_compo_weight NUMERIC(5,2);
