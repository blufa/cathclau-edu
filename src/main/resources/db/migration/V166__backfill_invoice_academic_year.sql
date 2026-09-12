-- Rattrapage des factures créées AVANT le correctif de résolution automatique de l'année scolaire
-- (InvoiceService.resolveAcademicYearId, session précédente) : les mensualités générées depuis la
-- fiche élève et les factures de transport auto-générées restaient avec academic_year_id NULL — donc
-- invisibles du recouvrement par classe et des plus gros débiteurs sur Vision Fondateur, qui filtre
-- strictement les factures par année scolaire (une classe pouvait ainsi apparaître à 100% de
-- recouvrement, seules ses quelques factures correctement taguées et déjà soldées restant visibles).
-- Même logique de repli que le correctif applicatif : l'inscription non-annulée la plus récente de
-- l'élève.
UPDATE invoices i
SET academic_year_id = (
    SELECT e.academic_year_id
    FROM enrollments e
    WHERE e.student_id = i.student_id AND e.status <> 'CANCELLED'
    ORDER BY e.id DESC
    LIMIT 1
)
WHERE i.academic_year_id IS NULL
  AND EXISTS (
    SELECT 1 FROM enrollments e2
    WHERE e2.student_id = i.student_id AND e2.status <> 'CANCELLED'
  );
