-- Snapshot du nom de l'élève payeur sur le reçu (imprimé sur le PDF), figé à l'émission.
ALTER TABLE receipts ADD COLUMN student_name VARCHAR(200);
