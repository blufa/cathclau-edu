-- La Garderie ne fait pas partie de la progression pédagogique de la Maternelle (pas de
-- Petite/Moyenne/Grande Section) : elle avait été créée avec cycle='MATERNELLE' par erreur
-- (catalogue de niveaux, level-catalog.tsx). Elle bascule vers son propre cycle GARDERIE, qui
-- déclenche toujours le badge de retrait obligatoire à la sortie (StudentAttendanceService).
UPDATE levels
SET cycle = 'GARDERIE'
WHERE cycle = 'MATERNELLE'
  AND label ILIKE 'garderie%';
