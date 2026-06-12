-- platform: plan archival flag (an archived plan is no longer subscribable but existing subscriptions remain valid)
ALTER TABLE plan
    ADD COLUMN IF NOT EXISTS archived BOOLEAN NOT NULL DEFAULT FALSE;
