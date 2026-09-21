-- Permet à chaque école de définir elle-même le format de génération de ses matricules (élèves et
-- enseignants), au lieu du format fixe SIGLE+5 chiffres. Nullable : une valeur absente conserve
-- exactement le comportement historique (voir gn.lacaeducation.common.MatriculeFormatter).
ALTER TABLE tenant ADD COLUMN matricule_format VARCHAR(50);
ALTER TABLE tenant ADD COLUMN matricule_digits INTEGER;
