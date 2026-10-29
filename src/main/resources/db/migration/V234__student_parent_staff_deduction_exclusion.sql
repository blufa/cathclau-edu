-- Retour comptable : certains enfants d'un même membre du personnel bénéficient de l'avantage
-- personnel (retenue sur salaire) tandis que d'autres enfants du MÊME salarié sont payés directement
-- par un parent — le réglage global sur la fiche Parent (exclude_from_auto_deduction, V231) s'applique
-- à TOUS les enfants de ce parent et ne permet pas ce mélange. Ce nouveau réglage est par lien
-- élève-parent (donc par enfant), en plus du réglage global (l'un OU l'autre suffit à exclure).
ALTER TABLE student_parents ADD COLUMN excluded_from_staff_deduction BOOLEAN NOT NULL DEFAULT false;
