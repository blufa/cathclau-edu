-- Retour comptable : certains membres du personnel paient réellement eux-mêmes les frais de leurs
-- enfants (comme n'importe quelle famille) — pour eux, aucune retenue automatique sur salaire ne doit
-- se déclencher. Réglable par le comptable sur la fiche Parent (jamais par le parent lui-même).
ALTER TABLE parents ADD COLUMN exclude_from_auto_deduction BOOLEAN NOT NULL DEFAULT false;
