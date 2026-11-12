-- Faille du QR de badge en clair (BADGE|tenant|type|id) : n'importe qui voyant ce texte pouvait
-- construire un QR valide pour N'IMPORTE QUEL identifiant, sans jamais avoir vu le vrai badge. Un
-- jeton opaque aléatoire remplace désormais le triplet lisible dans le QR : impossible à deviner ou à
-- construire sans posséder le badge réellement émis. Nullable : seuls les enrôlements « Badge QR » en
-- ont un, les appareils biométriques classiques (empreinte/visage) n'en ont pas besoin.
ALTER TABLE biometric_enrollments ADD COLUMN badge_token VARCHAR(64);
CREATE UNIQUE INDEX uq_bioenroll_badge_token ON biometric_enrollments (badge_token) WHERE badge_token IS NOT NULL;
