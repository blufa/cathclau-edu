-- Nettoyage : du temps où les entités EmploiDuTemps/ClasseMatiere mappaient par erreur
-- "school_class_id", Hibernate (ddl-auto=update) avait ajouté cette colonne (NOT NULL) à côté de la
-- vraie colonne "class_id". Les entités pointent désormais sur "class_id" ; on supprime l'orpheline,
-- sinon l'insert laisse "school_class_id" à null et viole la contrainte NOT NULL.
ALTER TABLE schedules      DROP COLUMN IF EXISTS school_class_id;
ALTER TABLE class_subjects DROP COLUMN IF EXISTS school_class_id;
