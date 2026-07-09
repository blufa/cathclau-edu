-- Rattache une période à un cycle (PRIMAIRE/COLLEGE/LYCEE/SUPERIEUR) : le primaire fonctionne en
-- trimestres, le secondaire/supérieur en semestres, au sein d'une même école.
ALTER TABLE periods ADD COLUMN cycle VARCHAR(20);
