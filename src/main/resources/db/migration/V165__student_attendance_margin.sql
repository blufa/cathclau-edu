-- Marge de tolérance séparée pour les élèves : la colonne existante `transition_margin_minutes` ne
-- sert plus qu'aux enseignants/personnel (temps de trajet entre deux séances) — les élèves ont
-- désormais leur propre marge, réglable indépendamment (§réglages présence). Même défaut à 0 (pas de
-- tolérance tant que l'école ne l'a pas explicitement configurée).
ALTER TABLE attendance_settings ADD COLUMN IF NOT EXISTS student_transition_margin_minutes INTEGER NOT NULL DEFAULT 0;
