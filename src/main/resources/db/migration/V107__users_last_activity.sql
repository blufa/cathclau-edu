-- Horodatage de la dernière requête authentifiée (mis à jour côté applicatif, avec throttling), pour
-- dériver un statut « en ligne / hors ligne » côté vue plateforme (SUPER_ADMIN), sans dépendre du seul
-- last_login_at qui ne bouge qu'à la connexion et pas pendant toute la durée de la session.
ALTER TABLE users ADD COLUMN last_activity_at TIMESTAMP;
