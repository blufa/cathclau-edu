-- Nouveau rôle métier : Responsable pédagogique (supervise la pédagogie sous la direction).
INSERT INTO roles (name, code, description)
VALUES ('Responsable pédagogique', 'RESPONSABLE_PEDAGOGIQUE', 'Supervision pédagogique')
ON CONFLICT (code) DO NOTHING;
