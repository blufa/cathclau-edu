package gn.lacaeducation.communication.adapters.out.persistence;

import gn.lacaeducation.communication.application.port.out.PaymentReminderDataPort.ClassFeeLine;
import gn.lacaeducation.communication.application.port.out.PaymentReminderDataPort.DueLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

interface PaymentReminderJpaRepository extends JpaRepository<CommunicationInvoiceMirrorEntity, Long> {
    // L'échéance (dueDate) vit sur l'installment lié, la classe sur le frais lié — ni l'un ni l'autre
    // sur la facture elle-même.
    @Query("SELECT new gn.lacaeducation.communication.application.port.out.PaymentReminderDataPort$DueLine("
         + "i.studentId, sf.classId, i.remainingBalance, inst.dueDate) "
         + "FROM CommunicationInvoiceMirrorEntity i "
         + "JOIN CommunicationInstallmentMirrorEntity inst ON i.installmentId = inst.id "
         + "LEFT JOIN CommunicationSchoolFeeMirrorEntity sf ON i.feeId = sf.id "
         + "WHERE i.tenantId = :tenantId AND i.status NOT IN ('PAID', 'CANCELLED') AND inst.dueDate < :today")
    List<DueLine> findOverdueLines(@Param("tenantId") Long tenantId, @Param("today") LocalDate today);

    @Query("SELECT new gn.lacaeducation.communication.application.port.out.PaymentReminderDataPort$DueLine("
         + "i.studentId, sf.classId, i.remainingBalance, inst.dueDate) "
         + "FROM CommunicationInvoiceMirrorEntity i "
         + "JOIN CommunicationInstallmentMirrorEntity inst ON i.installmentId = inst.id "
         + "LEFT JOIN CommunicationSchoolFeeMirrorEntity sf ON i.feeId = sf.id "
         + "WHERE i.tenantId = :tenantId AND i.status NOT IN ('PAID', 'CANCELLED') AND inst.dueDate BETWEEN :from AND :to")
    List<DueLine> findUpcomingLines(@Param("tenantId") Long tenantId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT new gn.lacaeducation.communication.application.port.out.PaymentReminderDataPort$ClassFeeLine("
         + "i.studentId, i.installmentId, i.remainingBalance, i.status, inst.dueDate) "
         + "FROM CommunicationInvoiceMirrorEntity i "
         + "JOIN CommunicationInstallmentMirrorEntity inst ON i.installmentId = inst.id "
         + "JOIN CommunicationSchoolFeeMirrorEntity sf ON i.feeId = sf.id "
         + "WHERE i.tenantId = :tenantId AND sf.classId = :classId AND sf.type = 'MONTHLY'")
    List<ClassFeeLine> findClassMonthlyFeeLines(@Param("tenantId") Long tenantId, @Param("classId") Long classId);

    @Query("SELECT COUNT(inst) FROM CommunicationInstallmentMirrorEntity inst "
         + "JOIN CommunicationSchoolFeeMirrorEntity sf ON inst.feeId = sf.id "
         + "WHERE sf.tenantId = :tenantId AND sf.classId = :classId AND sf.type = 'MONTHLY'")
    long countMonthlyTranches(@Param("tenantId") Long tenantId, @Param("classId") Long classId);
}
