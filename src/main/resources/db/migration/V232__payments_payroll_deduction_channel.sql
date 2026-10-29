-- Retenue sur salaire comme canal de paiement (§payroll ↔ billing) : permet de produire un reçu de
-- paiement pour un enfant de personnel dont les frais ont été retenus directement sur le salaire,
-- sans jamais passer par le guichet (voir PaymentUseCase#recordSystemPayment).
ALTER TABLE payments DROP CONSTRAINT payments_channel_check;
ALTER TABLE payments ADD CONSTRAINT payments_channel_check
    CHECK (channel IN ('ORANGE_MONEY','MTN_MONEY','WAVE','BANK_CARD','DJOMY','CASH','PAYROLL_DEDUCTION'));
