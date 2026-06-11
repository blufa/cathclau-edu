-- Add status column to installments table for overdue tracking
ALTER TABLE installments
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'PAID', 'OVERDUE'));

-- Index for efficient overdue queries
CREATE INDEX idx_installments_status_due_date ON installments(status, due_date);
