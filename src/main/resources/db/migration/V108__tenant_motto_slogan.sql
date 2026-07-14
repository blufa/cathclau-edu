-- Devise et slogan de l'établissement, imprimés au verso des badges/cartes (élèves, enseignants,
-- personnel) aux côtés du symbole national.
ALTER TABLE tenant ADD COLUMN motto TEXT;
ALTER TABLE tenant ADD COLUMN slogan TEXT;
