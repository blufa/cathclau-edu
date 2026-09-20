-- Enseignant exceptionnel pouvant couvrir plusieurs classes en même temps (ex. professeur d'EPS qui
-- rassemble plusieurs classes ensemble sur le même créneau, §5.6) — désactive la détection de conflit
-- d'emploi du temps sur cet enseignant précis, sans jamais toucher aux conflits de salle/classe.

ALTER TABLE teachers ADD COLUMN allows_concurrent_classes BOOLEAN NOT NULL DEFAULT FALSE;
