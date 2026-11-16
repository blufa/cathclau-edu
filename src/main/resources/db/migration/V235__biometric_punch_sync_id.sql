-- Pointage hors ligne (biometric) : identifiant de synchronisation généré côté appareil/agent,
-- permettant une ingestion idempotente (un renvoi après coupure réseau ne doit jamais dupliquer
-- un pointage déjà enregistré). Nullable : les agents existants qui n'envoient pas encore ce
-- champ continuent de fonctionner comme avant (chaque appel crée un pointage).
ALTER TABLE biometric_punches ADD COLUMN sync_id VARCHAR(64);

-- Unicité uniquement quand sync_id est renseigné (index partiel) : deux pointages legacy sans
-- sync_id (NULL) ne sont jamais considérés en conflit entre eux.
CREATE UNIQUE INDEX uq_biopunch_device_syncid ON biometric_punches (device_id, sync_id) WHERE sync_id IS NOT NULL;
