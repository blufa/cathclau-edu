-- Grading: seuils configurables des distinctions du conseil de classe (Félicitations / Compliments).
-- Rendus paramétrables par tenant ; auparavant codés en dur (16 / 13 / 10) dans le service de calcul.
ALTER TABLE grading_scales
    ADD COLUMN felicitations_min                 NUMERIC(5,2) NOT NULL DEFAULT 16,
    ADD COLUMN compliments_min                   NUMERIC(5,2) NOT NULL DEFAULT 13,
    ADD COLUMN distinction_no_failing_threshold  NUMERIC(5,2) NOT NULL DEFAULT 10;
