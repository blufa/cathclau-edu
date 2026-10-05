-- Numéro du fondateur/administrateur de l'école, dédié à la réception des alertes automatiques
-- (écart de caisse, exonération suspecte, présence sans inscription, retard/absence enseignant…).
-- Avant cette colonne, ces alertes partaient vers TOUS les comptes SCHOOL_ADMIN du tenant, qui ne
-- correspondent pas toujours au numéro du fondateur réel (§retour fondateurs). Null = on continue de
-- notifier les comptes SCHOOL_ADMIN (comportement historique), tant que ce numéro n'est pas renseigné.
ALTER TABLE tenant ADD COLUMN founder_phone VARCHAR(32);
