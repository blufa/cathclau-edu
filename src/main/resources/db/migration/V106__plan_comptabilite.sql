-- Plan « Comptabilité seule » : offre destinée aux écoles qui ne veulent que la partie finances.
-- Modules inclus : IAM (utilisateurs & rôles), ACCOUNTING (comptabilité générale, trésorerie, rapports)
-- et EXPENSES (dépenses & budget). Aucun module élèves/notes.
--
-- NB : les tarifs ci-dessous sont des valeurs par défaut à ajuster par le SUPER_ADMIN via l'écran des
-- plans. Migration idempotente (ré-exécutable sans doublon).
INSERT INTO plan (name, code, student_limit, monthly_price, annual_price, description,
                  is_trial, default_trial, plan_type, public_visible, highlighted, display_order)
SELECT 'Comptabilité', 'COMPTABILITE', NULL, 100000, 1000000,
       'Offre comptabilité seule : comptabilité générale, trésorerie, dépenses et budget. Sans gestion des élèves.',
       FALSE, FALSE, 'STANDARD', TRUE, FALSE, 10
WHERE NOT EXISTS (SELECT 1 FROM plan WHERE code = 'COMPTABILITE');

-- Modules du plan (IAM + Comptabilité + Dépenses).
INSERT INTO plan_module (plan_id, module_code, active)
SELECT p.id, m.code, TRUE
FROM plan p
CROSS JOIN (VALUES ('IAM'), ('ACCOUNTING'), ('EXPENSES')) AS m(code)
WHERE p.code = 'COMPTABILITE'
  AND NOT EXISTS (
      SELECT 1 FROM plan_module pm WHERE pm.plan_id = p.id AND pm.module_code = m.code
  );
