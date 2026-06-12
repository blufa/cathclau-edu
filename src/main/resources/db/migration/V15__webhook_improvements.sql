-- Add external_event_id to payment_webhooks for idempotency
ALTER TABLE payment_webhooks ADD COLUMN IF NOT EXISTS external_event_id VARCHAR(255);
CREATE UNIQUE INDEX IF NOT EXISTS idx_payment_webhooks_external_event_id ON payment_webhooks(external_event_id) WHERE external_event_id IS NOT NULL;

-- Add index for querying webhook_events by status only (used by WebhookEventCreator/replay)
CREATE INDEX IF NOT EXISTS idx_webhook_events_status ON webhook_events(status);

-- Ensure next_attempt_at and attempt_count have proper defaults in webhook_events
ALTER TABLE webhook_events ALTER COLUMN attempt_count SET DEFAULT 0;
