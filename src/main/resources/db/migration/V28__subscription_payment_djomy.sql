-- Câblage Djomy pour les paiements d'abonnement : stocke l'URL de redirection du portail.
ALTER TABLE subscription_payment ADD COLUMN redirect_url VARCHAR(1024);
