package gn.lacaeducation.students.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * Détection de doublons élèves au sein d'une même classe (§retour comptable : "il peut avoir deux à
 * trois mêmes prénoms, nom... sans [être] la même [personne] dans la même classe" — une homonymie
 * seule ne suffit jamais à conclure à un doublon). NATIVE : regroupe par classe + prénom/nom
 * normalisés (casse/espaces ignorés), puis agrège le profil financier RÉEL de chaque membre du groupe
 * (factures/encaissé/nombre de mensualités facturées) — c'est cet écart financier entre les membres
 * d'un même groupe, pas le seul nom, qui distingue un doublon probable (fiche quasi vide créée par
 * erreur) d'une simple homonymie (ex. jumeaux, chacun avec son propre historique complet de factures).
 */
interface DuplicateStudentJpaRepository extends JpaRepository<EleveJpaEntity, Long> {

    interface DuplicateCandidateRow {
        Long getStudentId();
        String getFirstName();
        String getLastName();
        String getPhoto();
        Long getClassId();
        String getClassName();
        BigDecimal getTotalInvoiced();
        BigDecimal getTotalPaid();
        Long getMonthlyInvoiceCount();
    }

    @Query(value = """
            WITH enrolled AS (
                SELECT DISTINCT ON (e.student_id) e.student_id, e.class_id
                FROM enrollments e
                WHERE e.status = 'VALIDATED'
                ORDER BY e.student_id, e.id DESC
            ),
            cand AS (
                SELECT s.id AS student_id, s.first_name, s.last_name, s.photo, en.class_id,
                       sc.label AS class_label,
                       LOWER(TRIM(s.first_name)) AS norm_first, LOWER(TRIM(s.last_name)) AS norm_last
                FROM students s
                JOIN enrolled en ON en.student_id = s.id
                JOIN school_classes sc ON sc.id = en.class_id
                WHERE s.tenant_id = :tenantId AND s.deleted_at IS NULL
            ),
            dupe_groups AS (
                SELECT class_id, norm_first, norm_last
                FROM cand
                GROUP BY class_id, norm_first, norm_last
                HAVING COUNT(*) > 1
            )
            SELECT c.student_id AS studentId, c.first_name AS firstName, c.last_name AS lastName,
                   c.photo AS photo, c.class_id AS classId, c.class_label AS className,
                   COALESCE(SUM(i.total_amount), 0) AS totalInvoiced,
                   COALESCE(SUM(i.paid_amount), 0) AS totalPaid,
                   COUNT(DISTINCT CASE WHEN sf.type = 'MONTHLY' THEN i.id END) AS monthlyInvoiceCount
            FROM cand c
            JOIN dupe_groups g ON g.class_id = c.class_id AND g.norm_first = c.norm_first AND g.norm_last = c.norm_last
            LEFT JOIN invoices i ON i.student_id = c.student_id
            LEFT JOIN school_fees sf ON sf.id = i.fee_id
            GROUP BY c.student_id, c.first_name, c.last_name, c.photo, c.class_id, c.class_label
            ORDER BY c.class_id, c.last_name, c.first_name
            """, nativeQuery = true)
    List<DuplicateCandidateRow> findDuplicateCandidates(@Param("tenantId") Long tenantId);
}
