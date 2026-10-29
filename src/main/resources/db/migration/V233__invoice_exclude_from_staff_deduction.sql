-- Facture annexe (ex. Maillots, T-shirts, Tenue scolaire) retirée au cas par cas de la retenue
-- automatique sur salaire (§retour utilisateur : "pas obligatoire de prendre tous les frais dans le
-- mois de sa fiche de paie") — le parent règle alors celle-ci lui-même, les autres factures de
-- l'enfant restent retenues normalement.
ALTER TABLE invoices ADD COLUMN excluded_from_staff_deduction BOOLEAN NOT NULL DEFAULT false;
