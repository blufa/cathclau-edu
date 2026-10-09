-- Suivi "déjà imprimé" sur la planche de badges (§studentcards, demande utilisateur) : NULL = jamais
-- imprimée, sinon horodatage du dernier passage sur une planche — sert à filtrer les cartes restant à
-- imprimer pour ne jamais réimprimer par erreur un badge déjà sorti.
ALTER TABLE student_card ADD COLUMN printed_at TIMESTAMP NULL;
