-- URL du PDF de la facture (généré à la demande puis stocké sur Cloudinary, §14).
-- Permet l'aperçu/téléchargement/impression côté front sans régénérer à chaque appel.
ALTER TABLE invoices ADD COLUMN pdf_file VARCHAR(1024);
