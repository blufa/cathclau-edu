-- Informations légales de l'école affichées sur les documents officiels (reçus, factures, bulletins) :
-- numéro d'agrément et référence de l'autorisation d'ouverture délivrée par le ministère.
ALTER TABLE tenant ADD COLUMN accreditation_number VARCHAR(120);
ALTER TABLE tenant ADD COLUMN opening_authorization VARCHAR(120);
