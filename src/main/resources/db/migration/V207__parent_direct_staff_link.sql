-- Un parent qui est aussi enseignant/personnel ne peut aujourd'hui être détecté comme tel que via un
-- compte utilisateur partagé (même userId que sa fiche Teacher/Employee) — impossible pour un membre
-- du personnel qui ne veut/ne peut pas avoir de compte (ex. pas de smartphone Android). Nouveau lien
-- DIRECT, sans compte requis : la fiche Parent référence directement la fiche Teacher/Employee.

ALTER TABLE parents ADD COLUMN linked_staff_type VARCHAR(20);
ALTER TABLE parents ADD COLUMN linked_staff_id BIGINT;
