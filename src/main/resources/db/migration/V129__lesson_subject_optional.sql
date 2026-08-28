-- Maternelle/primaire : un seul enseignant (« moniteur ») couvre toutes les matières de la classe en
-- continu jusqu'à l'heure de sortie — la séance du jour se crée sans choisir une matière précise.
ALTER TABLE lesson_entries ALTER COLUMN subject_id DROP NOT NULL;
