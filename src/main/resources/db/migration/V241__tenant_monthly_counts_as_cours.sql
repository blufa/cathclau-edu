-- §censeur, école visitée : certaines écoles ne font qu'une évaluation mensuelle par matière et
-- l'utilisent directement comme moyenne de cours du mois dans le calcul de la moyenne semestrielle
-- officielle — réglage par école (voir Tenant#monthlyCountsAsCours), désactivé par défaut pour
-- rester fidèle au comportement historique (MONTHLY toujours exclu du calcul semestriel).
ALTER TABLE tenants
    ADD COLUMN monthly_counts_as_cours BOOLEAN NOT NULL DEFAULT FALSE;
