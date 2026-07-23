-- Catégorie de frais annexe personnalisée (§4) : une école peut créer à la volée un frais "Autre"
-- (tenue scolaire, fournitures, bus...) nommé librement via la nouvelle colonne label.
ALTER TABLE school_fees ADD COLUMN label VARCHAR(120);

ALTER TABLE school_fees DROP CONSTRAINT school_fees_type_check;
ALTER TABLE school_fees ADD CONSTRAINT school_fees_type_check
    CHECK (type IN ('ENROLLMENT','RE_ENROLLMENT','MONTHLY','TRANSPORT','CANTEEN','EXAM','OTHER'));
