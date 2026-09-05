-- Identifiant du message chez le fournisseur (ex. "messageid" NimbaSMS) — nécessaire pour recaler le
-- statut de livraison quand le fournisseur rappelle via webhook (§communication).
ALTER TABLE message_recipients ADD COLUMN IF NOT EXISTS provider_message_id VARCHAR(100);
CREATE INDEX IF NOT EXISTS idx_message_recipients_provider_message_id
    ON message_recipients (provider_message_id) WHERE provider_message_id IS NOT NULL;
