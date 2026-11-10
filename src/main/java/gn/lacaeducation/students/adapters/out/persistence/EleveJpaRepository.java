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
     * (nom/prénom/téléphone du parent lié, case-insensitive), by class and/or enrollment status
     * (through the enrollments table), and by transport subscription status / number of months of
     * transport already paid (through the transport_subscriptions / invoices / school_fees tables,
     * mirrored read-only in this module — §retour comptable : "filtrer par transport, pour chercher
     * rapidement"). {@code q} must be passed already lower-cased. {@code minPaidMonths = 0} means
     * "aucun mois payé" (exact match), any other non-null value means "au moins N mois payés".
     */
    @Query("""
            SELECT s FROM EleveJpaEntity s
            WHERE s.tenantId = :tenantId
              AND (:gender IS NULL OR s.gender = :gender)
              AND (:q IS NULL OR LOWER(s.firstName) LIKE CONCAT('%', CAST(:q AS string), '%')
                              OR LOWER(s.lastName) LIKE CONCAT('%', CAST(:q AS string), '%')
                              OR LOWER(s.studentNumber) LIKE CONCAT('%', CAST(:q AS string), '%')
                              OR LOWER(CONCAT(s.firstName, ' ', s.lastName)) LIKE CONCAT('%', CAST(:q AS string), '%')
                              OR LOWER(CONCAT(s.lastName, ' ', s.firstName)) LIKE CONCAT('%', CAST(:q AS string), '%')
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
              AND (:transportSubscribed IS NULL
                   OR (:transportSubscribed = TRUE AND EXISTS (
                        SELECT 1 FROM StudentsTransportSubscriptionMirrorEntity sub
                        WHERE sub.studentId = s.id AND sub.status = 'ACTIVE'))
                   OR (:transportSubscribed = FALSE AND NOT EXISTS (
                        SELECT 1 FROM StudentsTransportSubscriptionMirrorEntity sub
                        WHERE sub.studentId = s.id AND sub.status = 'ACTIVE')))
              AND (:minPaidMonths IS NULL
                   OR (:minPaidMonths = 0 AND (
                        SELECT COUNT(i) FROM StudentsInvoiceMirrorEntity i
                        WHERE i.studentId = s.id AND i.paidAmount > 0
                          AND i.feeId IN (SELECT f.id FROM StudentsSchoolFeeMirrorEntity f WHERE f.type = 'TRANSPORT')
                       ) = 0)
                   OR (:minPaidMonths > 0 AND (
                        SELECT COUNT(i) FROM StudentsInvoiceMirrorEntity i
                        WHERE i.studentId = s.id AND i.paidAmount > 0
                          AND i.feeId IN (SELECT f.id FROM StudentsSchoolFeeMirrorEntity f WHERE f.type = 'TRANSPORT')
                       ) >= :minPaidMonths))
            """)
    Page<EleveJpaEntity> search(@Param("tenantId") Long tenantId,
                                 @Param("gender") Gender gender,
                                 @Param("q") String q,
                                 @Param("classId") Long classId,
                                 @Param("status") EnrollmentStatus status,
                                 @Param("notEnrolled") boolean notEnrolled,
                                 @Param("transportSubscribed") Boolean transportSubscribed,
                                 @Param("minPaidMonths") Integer minPaidMonths,
                                 Pageable pageable);

    /**
     * Suggestions de prénom/nom/adresse déjà saisis pour d'autres élèves du même tenant (§retour
     * comptable : fratries, adresses d'un même quartier) — {@code q} déjà en minuscules.
     */
    @Query("SELECT DISTINCT s.firstName FROM EleveJpaEntity s WHERE s.tenantId = :tenantId "
            + "AND LOWER(s.firstName) LIKE CONCAT('%', CAST(:q AS string), '%') ORDER BY s.firstName")
    List<String> suggestFirstNames(@Param("tenantId") Long tenantId, @Param("q") String q, Pageable pageable);

    @Query("SELECT DISTINCT s.lastName FROM EleveJpaEntity s WHERE s.tenantId = :tenantId "
            + "AND LOWER(s.lastName) LIKE CONCAT('%', CAST(:q AS string), '%') ORDER BY s.lastName")
    List<String> suggestLastNames(@Param("tenantId") Long tenantId, @Param("q") String q, Pageable pageable);

    @Query("SELECT DISTINCT s.address FROM EleveJpaEntity s WHERE s.tenantId = :tenantId AND s.address IS NOT NULL "
            + "AND LOWER(s.address) LIKE CONCAT('%', CAST(:q AS string), '%') ORDER BY s.address")
    List<String> suggestAddresses(@Param("tenantId") Long tenantId, @Param("q") String q, Pageable pageable);
}
