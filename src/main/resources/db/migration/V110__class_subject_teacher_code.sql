-- Code enseignant par matière (ex. "CH1", "CH2" pour le 1er/2e professeur de chimie), affecté par
-- le Directeur des Études pour identifier rapidement un professeur dans la grille d'emploi du temps.
ALTER TABLE class_subjects ADD COLUMN code VARCHAR(20);
