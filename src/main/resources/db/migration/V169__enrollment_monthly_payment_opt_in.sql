-- Un parent peut choisir de régler mois par mois plutôt que par tranche groupée (§billing) — même
-- pattern que platform_access_opt_in : opt-in par inscription, modifiable après coup, défaut false
-- (comportement groupé standard de la classe inchangé pour tous les autres élèves).
ALTER TABLE enrollments ADD COLUMN IF NOT EXISTS monthly_payment_opt_in BOOLEAN NOT NULL DEFAULT false;
