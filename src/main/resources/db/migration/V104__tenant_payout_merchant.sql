-- Numéro marchand de reversement par école (modèle : encaissement sur le compte plateforme Djomy,
-- puis reversement à l'école sur son propre compte Mobile Money). Ces champs ne servent PAS à
-- l'encaissement (qui reste sur le compte plateforme) mais au versement des sommes dues à l'école.
ALTER TABLE tenant ADD COLUMN payout_merchant_number VARCHAR(40);
ALTER TABLE tenant ADD COLUMN payout_operator        VARCHAR(30);
ALTER TABLE tenant ADD COLUMN payout_account_name    VARCHAR(160);
