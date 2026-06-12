package gn.lacaeducation.students.adapters.out.persistence;

import gn.lacaeducation.students.domain.EnrollmentStatus;
import gn.lacaeducation.students.domain.Gender;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

interface EleveJpaRepository extends JpaRepository<EleveJpaEntity, Long> {
    List<EleveJpaEntity> findByTenantId(Long tenantId);
    long countByTenantId(Long tenantId);
    boolean existsByTenantIdAndStudentNumber(Long tenantId, String studentNumber);

    /**
     * Paginated student search scoped to a tenant, filtering by gender, name/matricule (case-insensitive)
     * and — through the enrollments table — by class and/or enrollment status.
     * {@code q} must be passed already lower-cased.
     */
    @Query("""
            SELECT s FROM EleveJpaEntity s
            WHERE s.tenantId = :tenantId
              AND (:gender IS NULL OR s.gender = :gender)
              AND (:q IS NULL OR LOWER(s.firstName) LIKE CONCAT('%', :q, '%')
                              OR LOWER(s.lastName) LIKE CONCAT('%', :q, '%')
                              OR LOWER(s.studentNumber) LIKE CONCAT('%', :q, '%'))
              AND ((:classId IS NULL AND :status IS NULL)
                   OR EXISTS (SELECT 1 FROM InscriptionJpaEntity e
                              WHERE e.studentId = s.id
                                AND (:classId IS NULL OR e.classId = :classId)
                                AND (:status IS NULL OR e.status = :status)))
            """)
    Page<EleveJpaEntity> search(@Param("tenantId") Long tenantId,
                                 @Param("gender") Gender gender,
                                 @Param("q") String q,
                                 @Param("classId") Long classId,
                                 @Param("status") EnrollmentStatus status,
                                 Pageable pageable);
}
