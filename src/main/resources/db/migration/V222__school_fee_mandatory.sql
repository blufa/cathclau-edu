-- Frais annexes obligatoire/facultatif (§13 cahier des charges) : jusqu'ici, TOUS les frais d'une
-- classe étaient facturés automatiquement à l'inscription, sans distinction. Un frais facultatif
-- (ex. paquet de RAM) ne doit plus être facturé/encaissé automatiquement — le comptable l'ajoute
-- explicitement à l'élève concerné s'il le souhaite.
-- Valeur par défaut TRUE (obligatoire) pour préserver exactement le comportement actuel sur TOUS les
-- frais déjà configurés : rien ne change pour l'existant tant qu'un frais n'est pas explicitement
-- repassé à "facultatif" dans l'écran de configuration.
ALTER TABLE school_fees ADD COLUMN mandatory BOOLEAN NOT NULL DEFAULT TRUE;
