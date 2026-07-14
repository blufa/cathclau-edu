-- Agent ayant physiquement encaissé un paiement en espèces (« opérateur / caissier »), pour
-- l'impression du reçu officiel. Null pour les canaux Mobile Money (pas d'agent humain).
ALTER TABLE payments ADD COLUMN collected_by_user_id BIGINT;
-- Nom de l'agent figé au moment de l'émission du reçu (snapshot, comme student_name), pour rester
-- stable dans le temps même si le compte est ensuite renommé/désactivé.
ALTER TABLE receipts ADD COLUMN collected_by_name VARCHAR(200);
