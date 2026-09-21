-- Suppression de l'ancien moteur de calcul de moyenne paramétrable (formules par étapes
-- GROUP/COMBINE, cf. V93/V115/V121) — remplacé par une formule fixe secondaire/primaire
-- (voir gn.lacaeducation.grading.domain.SubjectSemesterAverage). Aucun tenant en production n'avait
-- configuré de formule personnalisée à ce jour (fonctionnalité jamais exposée en avant-plan).
-- Ordre : tables dépendantes (FK) d'abord.
-- Les colonnes report_card_lines.cours_average / compo_average (ajoutées par V121) sont CONSERVÉES —
-- toujours utilisées par le nouveau moteur, aucune suppression de colonne ici.
DROP TABLE IF EXISTS grading_formula_step_input;
DROP TABLE IF EXISTS grading_formula_step;
DROP TABLE IF EXISTS grading_formula;
DROP TABLE IF EXISTS grading_calculation_settings;
