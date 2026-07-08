-- Ajoute le canal CASH (encaissement direct au guichet, sans fournisseur) à la contrainte
-- des paiements scolaires. L'enum PaymentChannel a gagné la valeur CASH mais le CHECK
-- en base la refusait (violation payments_channel_check à l'insertion).
ALTER TABLE payments DROP CONSTRAINT payments_channel_check;
ALTER TABLE payments ADD CONSTRAINT payments_channel_check
    CHECK (channel IN ('ORANGE_MONEY','MTN_MONEY','WAVE','BANK_CARD','DJOMY','CASH'));
