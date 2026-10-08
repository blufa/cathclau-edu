-- Retour comptable : savoir QUAND le crédit a été reporté (ex. transfert de classe), pour l'afficher
-- à la place du "-" dans la colonne "Date & heure" du reçu quand aucun paiement direct n'existe.
ALTER TABLE invoices ADD COLUMN credited_at TIMESTAMP;
