-- Module 14 (level 2): SYSCOHADA double-entry — chart of accounts, journals, entries and lines.
CREATE TABLE IF NOT EXISTS chart_of_account (
    id             BIGSERIAL PRIMARY KEY,
    tenant_id      BIGINT NOT NULL,
    account_number VARCHAR(20) NOT NULL,
    label          VARCHAR(200) NOT NULL,
    account_class  INT,
    type           VARCHAR(12),
    parent_id      BIGINT REFERENCES chart_of_account (id),
    lettrable      BOOLEAN,
    active         BOOLEAN
);
CREATE INDEX IF NOT EXISTS idx_coa_tenant ON chart_of_account (tenant_id);

CREATE TABLE IF NOT EXISTS accounting_journal (
    id                            BIGSERIAL PRIMARY KEY,
    tenant_id                     BIGINT NOT NULL,
    code                          VARCHAR(10) NOT NULL,
    label                         VARCHAR(160),
    type                          VARCHAR(40),
    default_counterpart_account_id BIGINT
);
CREATE INDEX IF NOT EXISTS idx_journal_tenant ON accounting_journal (tenant_id);

CREATE TABLE IF NOT EXISTS journal_entry (
    id             BIGSERIAL PRIMARY KEY,
    tenant_id      BIGINT NOT NULL,
    fiscal_year_id BIGINT,
    journal_id     BIGINT,
    piece_number   VARCHAR(40),
    date           DATE,
    label          VARCHAR(255),
    reference_type VARCHAR(40),
    reference_id   BIGINT,
    status         VARCHAR(12) NOT NULL DEFAULT 'DRAFT',
    total_debit    NUMERIC(15, 2),
    total_credit   NUMERIC(15, 2)
);
CREATE INDEX IF NOT EXISTS idx_journal_entry_tenant ON journal_entry (tenant_id, fiscal_year_id);

CREATE TABLE IF NOT EXISTS journal_entry_line (
    id                  BIGSERIAL PRIMARY KEY,
    journal_entry_id    BIGINT REFERENCES journal_entry (id),
    chart_of_account_id BIGINT NOT NULL,
    label               VARCHAR(255),
    debit               NUMERIC(15, 2),
    credit              NUMERIC(15, 2),
    lettrage_code       VARCHAR(40),
    tiers_type          VARCHAR(20),
    tiers_id            BIGINT
);
CREATE INDEX IF NOT EXISTS idx_journal_entry_line_entry ON journal_entry_line (journal_entry_id);
