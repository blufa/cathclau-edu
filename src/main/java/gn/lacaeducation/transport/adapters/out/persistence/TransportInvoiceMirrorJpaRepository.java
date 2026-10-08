package gn.lacaeducation.transport.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

interface TransportInvoiceMirrorJpaRepository extends JpaRepository<TransportInvoiceMirrorEntity, Long> {

    @Query("SELECT DISTINCT i.studentId FROM TransportInvoiceMirrorEntity i "
            + "WHERE i.tenantId = :tenantId AND i.paidAmount > 0 "
            + "AND i.feeId IN (SELECT f.id FROM TransportSchoolFeeMirrorEntity f WHERE f.tenantId = :tenantId AND f.type = 'TRANSPORT')")
    List<Long> findStudentIdsWithPaidTransportInvoice(@Param("tenantId") Long tenantId);
}
