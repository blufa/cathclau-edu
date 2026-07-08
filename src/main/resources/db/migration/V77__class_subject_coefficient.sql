-- Coefficient de la matière PAR CLASSE (varie selon le niveau : ex. Chimie coef 2 en 10e, 4 en Terminale).
-- Null = on retombe sur le coefficient par défaut de la matière.
ALTER TABLE class_subjects ADD COLUMN IF NOT EXISTS coefficient NUMERIC(5, 2);
