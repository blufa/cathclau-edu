-- Chef de classe (§academics) : un élève inscrit (inscription validée) désigné pour scanner les
-- badges de ses camarades le matin. Géré séparément de la mise à jour générale de la classe pour ne
-- jamais être écrasé par inadvertance par le formulaire d'édition de classe.
ALTER TABLE school_classes ADD COLUMN IF NOT EXISTS class_representative_student_id BIGINT;
