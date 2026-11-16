-- Support NFC du badge (en plus du QR) : chaque enrôlement « Badge » peut associer l'UID matériel
-- d'une carte NFC physique, pointant vers la même personne que le jeton QR déjà présent — les deux
-- technologies cohabitent sur un même badge (§suivi-examen, BADGES_QR_NFC.md). Nullable : seuls les
-- badges effectivement munis d'une carte NFC en ont un.
ALTER TABLE biometric_enrollments ADD COLUMN nfc_uid VARCHAR(64);
CREATE UNIQUE INDEX uq_bioenroll_nfc_uid ON biometric_enrollments (nfc_uid) WHERE nfc_uid IS NOT NULL;
