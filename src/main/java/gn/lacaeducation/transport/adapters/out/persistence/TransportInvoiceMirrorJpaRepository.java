package gn.lacaeducation.transport.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

interface TransportInvoiceMirrorJpaRepository extends JpaRepository<TransportInvoiceMirrorEntity, Long> {

    /** Une ligne {@code [studentId, count]} par élève ayant réglé au moins un mois (jamais une ligne
     * à 0 — voir {@link TransportInvoiceQueryAdapter}, qui convertit en Map). */
    @Query("SELECT i.studentId, COUNT(i) FROM TransportInvoiceMirrorEntity i "
            + "WHERE i.tenantId = :tenantId AND i.paidAmount > 0 "
            + "AND i.feeId IN (SELECT f.id FROM TransportSchoolFeeMirrorEntity f WHERE f.tenantId = :tenantId AND f.type = 'TRANSPORT') "
            + "GROUP BY i.studentId")
    List<Object[]> countPaidTransportMonthsByStudentId(@Param("tenantId") Long tenantId);
}
