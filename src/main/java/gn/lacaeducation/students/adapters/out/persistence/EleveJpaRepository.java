package gn.lacaeducation.students.adapters.out.persistence;

import gn.lacaeducation.students.domain.EnrollmentStatus;
import gn.lacaeducation.students.domain.Gender;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

interface EleveJpaRepository extends JpaRepository<EleveJpaEntity, Long> {
    List<EleveJpaEntity> findByTenantId(Long tenantId);
    Optional<EleveJpaEntity> findByUserId(Long userId);
    Optional<EleveJpaEntity> findByIdempotencyKey(String idempotencyKey);
    long countByTenantId(Long tenantId);
    boolean existsByTenantIdAndStudentNumber(Long tenantId, String studentNumber);

    /**
     * Paginated student search scoped to a tenant, filtering by gender, name/matricule/parent
     * (nom/prénom/téléphone du parent lié, case-insensitive) and — through the enrollments table —
     * by class and/or enrollment status. {@code q} must be passed already lower-cased.
     */
    @Query("""
            SELECT s FROM EleveJpaEntity s
            WHERE s.tenantId = :tenantId
              AND (:gender IS NULL OR s.gender = :gender)
              AND (:q IS NULL OR LOWER(s.firstName) LIKE CONCAT('%', CAST(:q AS string), '%')
                              OR LOWER(s.lastName) LIKE CONCAT('%', CAST(:q AS string), '%')
                              OR LOWER(s.studentNumber) LIKE CONCAT('%', CAST(:q AS string), '%')
                              OR EXISTS (SELECT 1 FROM EleveParentJpaEntity ep JOIN ParentJpaEntity p ON p.id = ep.parentId
                                         WHERE ep.studentId = s.id
                                           AND (LOWER(p.firstName) LIKE CONCAT('%', CAST(:q AS string), '%')
                                                OR LOWER(p.lastName) LIKE CONCAT('%', CAST(:q AS string), '%')
                                                OR LOWER(p.phone) LIKE CONCAT('%', CAST(:q AS string), '%'))))
              AND ((:classId IS NULL AND :status IS NULL)
                   OR EXISTS (SELECT 1 FROM InscriptionJpaEntity e
                              WHERE e.studentId = s.id
                                AND (:classId IS NULL OR e.classId = :classId)
                                AND (:status IS NULL OR e.status = :status)))
              AND (:notEnrolled = FALSE OR NOT EXISTS (
                   SELECT 1 FROM InscriptionJpaEntity e3
                   WHERE e3.studentId = s.id
                     AND e3.status <> gn.lacaeducation.students.domain.EnrollmentStatus.CANCELLED))
            """)
    Page<EleveJpaEntity> search(@Param("tenantId") Long tenantId,
                                 @Param("gender") Gender gender,
                                 @Param("q") String q,
                                 @Param("classId") Long classId,
                                 @Param("status") EnrollmentStatus status,
                                 @Param("notEnrolled") boolean notEnrolled,
                                 Pageable pageable);
}
