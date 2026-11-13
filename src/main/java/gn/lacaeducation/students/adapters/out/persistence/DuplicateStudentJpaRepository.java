package gn.lacaeducation.students.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * Détection de doublons élèves (§retour comptable, requête élargie : "il ya doublure c'est lorsque
 * deux ou trois enfants on meme firstName, meme lastName, et meme lastName du parent" — plus limité à
 * la même classe comme avant : deux fiches du même élève peuvent être réparties sur deux classes
 * différentes). NATIVE : regroupe par prénom/nom élève + nom de famille d'UN des parents (normalisés,
 * casse/espaces ignorés — un élève avec plusieurs parents de noms différents peut matcher via l'un ou
 * l'autre), puis agrège le profil financier RÉEL de chaque membre du groupe (factures/encaissé/nombre
 * de mensualités facturées) — c'est cet écart financier entre les membres d'un même groupe, pas le
 * seul nom, qui distingue un doublon probable (fiche quasi vide créée par erreur) d'une simple
 * homonymie (ex. jumeaux, chacun avec son propre historique complet de factures).
 */
interface DuplicateStudentJpaRepository extends JpaRepository<EleveJpaEntity, Long> {

    interface DuplicateCandidateRow {
        Long getStudentId();
        String getFirstName();
        String getLastName();
        String getPhoto();
        Long getClassId();
        String getClassName();
        String getParentLastName();
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
            student_parent_name AS (
                SELECT DISTINCT sp.student_id, LOWER(TRIM(p.last_name)) AS norm_parent_last, p.last_name
                FROM student_parents sp
                JOIN parents p ON p.id = sp.parent_id
                WHERE p.last_name IS NOT NULL AND TRIM(p.last_name) <> ''
            ),
            cand AS (
                SELECT s.id AS student_id, s.first_name, s.last_name, s.photo, en.class_id,
                       sc.label AS class_label, spn.last_name AS parent_last_name,
                       LOWER(TRIM(s.first_name)) AS norm_first, LOWER(TRIM(s.last_name)) AS norm_last,
                       spn.norm_parent_last
                FROM students s
                JOIN student_parent_name spn ON spn.student_id = s.id
                LEFT JOIN enrolled en ON en.student_id = s.id
                LEFT JOIN school_classes sc ON sc.id = en.class_id
                WHERE s.tenant_id = :tenantId AND s.deleted_at IS NULL
            ),
            dupe_groups AS (
                SELECT norm_first, norm_last, norm_parent_last
                FROM cand
                GROUP BY norm_first, norm_last, norm_parent_last
                HAVING COUNT(DISTINCT student_id) > 1
            )
            SELECT c.student_id AS studentId, c.first_name AS firstName, c.last_name AS lastName,
                   c.photo AS photo, c.class_id AS classId, c.class_label AS className,
                   c.parent_last_name AS parentLastName,
                   COALESCE(SUM(i.total_amount), 0) AS totalInvoiced,
                   COALESCE(SUM(i.paid_amount), 0) AS totalPaid,
                   COUNT(DISTINCT CASE WHEN sf.type = 'MONTHLY' THEN i.id END) AS monthlyInvoiceCount
            FROM cand c
            JOIN dupe_groups g ON g.norm_first = c.norm_first AND g.norm_last = c.norm_last AND g.norm_parent_last = c.norm_parent_last
            LEFT JOIN invoices i ON i.student_id = c.student_id
            LEFT JOIN school_fees sf ON sf.id = i.fee_id
            GROUP BY c.student_id, c.first_name, c.last_name, c.photo, c.class_id, c.class_label, c.parent_last_name
            ORDER BY c.last_name, c.first_name, c.student_id
            """, nativeQuery = true)
    List<DuplicateCandidateRow> findDuplicateCandidates(@Param("tenantId") Long tenantId);
}
