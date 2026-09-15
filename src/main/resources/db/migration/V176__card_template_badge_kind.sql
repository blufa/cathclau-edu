-- Généralise les modèles de carte (jusqu'ici scopés élève) aux badges enseignant/personnel : chaque
-- modèle porte désormais un type de badge, et un tenant peut avoir un modèle par défaut PAR TYPE
-- (au lieu d'un seul défaut global) — voir CardTemplateService.setDefault.
ALTER TABLE card_template ADD COLUMN badge_kind VARCHAR(10) NOT NULL DEFAULT 'STUDENT';
