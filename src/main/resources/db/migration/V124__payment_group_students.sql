-- Élèves couverts par un paiement groupé (§payments) : un parent avec plusieurs enfants peut régler
-- inscription + scolarité + frais annexes de PLUSIEURS enfants en un seul paiement / un seul reçu.
-- Stockée en CSV entouré de virgules (ex. ",12,45,") pour permettre une recherche d'appartenance
-- fiable par LIKE ('%,45,%') sans confondre l'id 4 avec 45. student_id reste le "principal" (premier
-- enfant) pour compatibilité avec le code existant à paiement mono-élève.
ALTER TABLE payments ADD COLUMN IF NOT EXISTS student_ids TEXT;
