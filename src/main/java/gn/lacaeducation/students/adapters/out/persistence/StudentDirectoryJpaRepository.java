package gn.lacaeducation.students.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Cross-tenant student directory (features §E). A NATIVE query is used on purpose: native SQL is not
 * affected by the Hibernate {@code tenantFilter}, so school B can look up a student registered in
 * school A. Exposes only minimal fields; results are capped.
 */
interface StudentDirectoryJpaRepository extends JpaRepository<EleveJpaEntity, Long> {

    interface DirectoryRow {
        String getFirstName();
        String getLastName();
        String getStudentNumber();
        java.time.LocalDate getBirthDate();
        String getOriginSchool();
        String getSchoolName();
        String getLastClass();
        String getLastDecision();
        Long getUnpaidMonths();
        java.math.BigDecimal getOverallAverage();
    }

    @Query(value = """
            SELECT s.first_name AS firstName, s.last_name AS lastName, s.student_number AS studentNumber,
                   s.birth_date AS birthDate, s.origin_school AS originSchool, t.school_name AS schoolName,
                   (SELECT c.label FROM enrollments e JOIN school_classes c ON c.id = e.class_id
                     WHERE e.student_id = s.id ORDER BY e.id DESC LIMIT 1) AS lastClass,
                   (SELECT rc.decision FROM report_cards rc
                     JOIN periods p ON p.id = rc.period_id
                     WHERE rc.student_id = s.id AND rc.decision IS NOT NULL AND rc.decision <> ''
                     ORDER BY p.academic_year_id DESC, p.sort_order DESC, rc.id DESC LIMIT 1) AS lastDecision,
                   (SELECT COUNT(*) FROM invoices i
                     WHERE i.student_id = s.id AND i.installment_id IS NOT NULL AND i.status <> 'PAID') AS unpaidMonths,
                   (SELECT rc.overall_average FROM report_cards rc
                     JOIN periods p ON p.id = rc.period_id
                     WHERE rc.student_id = s.id AND rc.overall_average IS NOT NULL
                     ORDER BY p.academic_year_id DESC, p.sort_order DESC, rc.id DESC LIMIT 1) AS overallAverage
            FROM students s
            JOIN tenant t ON t.id = s.tenant_id
            WHERE s.deleted_at IS NULL
              AND ( (:matricule <> '' AND s.student_number = :matricule)
                    OR (:nameQuery <> '' AND (LOWER(s.first_name) LIKE CONCAT('%', :nameQuery, '%')
                                           OR LOWER(s.last_name) LIKE CONCAT('%', :nameQuery, '%'))) )
            ORDER BY s.last_name, s.first_name
            LIMIT 25
            """, nativeQuery = true)
    List<DirectoryRow> searchDirectory(@Param("matricule") String matricule, @Param("nameQuery") String nameQuery);

    /**
     * Noms d'écoles de la plateforme correspondant à la recherche (pour renseigner l'école d'origine
     * d'un élève transféré). NATIVE : non affecté par le tenantFilter ; ne renvoie que des noms.
     */
    @Query(value = """
            SELECT DISTINCT t.school_name FROM tenant t
            WHERE t.school_name IS NOT NULL
              AND LOWER(t.school_name) LIKE CONCAT('%', :q, '%')
            ORDER BY t.school_name
            LIMIT 25
            """, nativeQuery = true)
    List<String> searchSchools(@Param("q") String q);

    /**
     * Libellés de "dernière classe" déjà saisis sur la plateforme (ex. "CM2", "10ème") correspondant à
     * la recherche — pour suggérer une valeur déjà utilisée par un élève transféré, plutôt qu'un format
     * différent ressaisi à chaque fois. NATIVE, cross-tenant comme {@link #searchSchools} : ce sont de
     * simples libellés de niveau, sans donnée personnelle.
     */
    @Query(value = """
            SELECT DISTINCT s.previous_class FROM students s
            WHERE s.previous_class IS NOT NULL AND s.previous_class <> ''
              AND LOWER(s.previous_class) LIKE CONCAT('%', :q, '%')
            ORDER BY s.previous_class
            LIMIT 25
            """, nativeQuery = true)
    List<String> searchPreviousClasses(@Param("q") String q);
}
