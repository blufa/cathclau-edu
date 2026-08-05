-- Nettoyage de dérive de schéma : sur certaines bases, la colonne "hourly_rate" (remplacée par
-- hourly_rate_normal/hourly_rate_exam depuis V114) est restée en place, NOT NULL sans défaut,
-- bloquant tout nouvel INSERT sur teacher_compensation. Sans effet si la colonne n'existe plus.
ALTER TABLE teacher_compensation DROP COLUMN IF EXISTS hourly_rate;
