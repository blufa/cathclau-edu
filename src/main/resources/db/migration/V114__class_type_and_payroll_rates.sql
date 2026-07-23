-- Typologie de classe (§6 paie) : une classe normale ou d'examen, pour appliquer le tarif horaire
-- correspondant à l'enseignant qui y intervient.
ALTER TABLE school_classes ADD COLUMN class_type VARCHAR(20) NOT NULL DEFAULT 'NORMALE'
    CHECK (class_type IN ('NORMALE','EXAMEN'));

-- Rémunération : un tarif horaire par typologie de classe plutôt qu'un tarif unique.
ALTER TABLE teacher_compensation RENAME COLUMN hourly_rate TO hourly_rate_normal;
ALTER TABLE teacher_compensation ADD COLUMN hourly_rate_exam NUMERIC(15,2) NOT NULL DEFAULT 0;
