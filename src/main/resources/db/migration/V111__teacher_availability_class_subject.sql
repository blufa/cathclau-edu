-- La saisie d'une disponibilité doit désormais préciser la classe et la matière concernées, pour
-- éviter les doubles réservations et faciliter la génération automatique de l'emploi du temps.
-- Colonnes nullables (les disponibilités existantes n'ont pas ces informations) ; l'obligation de
-- les renseigner est appliquée en couche service pour les nouvelles saisies.
ALTER TABLE teacher_availability ADD COLUMN school_class_id BIGINT;
ALTER TABLE teacher_availability ADD COLUMN subject_id BIGINT;
