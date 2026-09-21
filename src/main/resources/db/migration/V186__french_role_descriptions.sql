-- V185 a traduit les noms des rôles système mais pas leur description ("Financial management" restait
-- visible sous « Comptable » sur l'écran Rôles & permissions — repéré par l'école).

UPDATE roles SET description = 'Gestion de la plateforme' WHERE code = 'SUPER_ADMIN';
UPDATE roles SET description = 'Gestion complète de l''école' WHERE code = 'SCHOOL_ADMIN';
UPDATE roles SET description = 'Direction pédagogique' WHERE code = 'DIRECTOR';
UPDATE roles SET description = 'Gestion financière' WHERE code = 'ACCOUNTANT';
UPDATE roles SET description = 'Secrétariat et inscriptions' WHERE code = 'SECRETARY';
UPDATE roles SET description = 'Saisie des notes et présences' WHERE code = 'TEACHER';
UPDATE roles SET description = 'Consultation des informations de son enfant' WHERE code = 'PARENT';
UPDATE roles SET description = 'Accès à son espace personnel' WHERE code = 'STUDENT';
