-- §retour comptable : la détection automatique "avantage personnel" (StaffChildBenefitPolicy) est
-- retirée du calcul des factures — tout devient manuel via le catalogue d'exonération. Ce drapeau
-- permet au comptable de marquer une exonération/un type comme "avantage personnel" pour que la
-- retenue sur salaire (§payroll, SalaryFeeDeductionService) continue de fonctionner malgré l'octroi
-- manuel (elle filtre les factures par DiscountSource.STAFF_BENEFIT).
ALTER TABLE exemptions ADD COLUMN staff_benefit BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE exemption_rules ADD COLUMN staff_benefit BOOLEAN NOT NULL DEFAULT false;
