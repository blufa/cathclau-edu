package gn.lacaeducation.transport.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

interface TransportInvoiceMirrorJpaRepository extends JpaRepository<TransportInvoiceMirrorEntity, Long> {

    /** Une ligne {@code [studentId, count]} par élève ayant réglé au moins un mois (jamais une ligne
     * à 0 — voir {@link TransportInvoiceQueryAdapter}, qui convertit en Map). */
    @Query("SELECT i.studentId, COUNT(i) FROM TransportInvoiceMirrorEntity i "
            + "WHERE i.tenantId = :tenantId AND i.paidAmount > 0 "
            + "AND i.feeId IN (SELECT f.id FROM TransportSchoolFeeMirrorEntity f WHERE f.tenantId = :tenantId AND f.type = 'TRANSPORT') "
            + "GROUP BY i.studentId")
    List<Object[]> countPaidTransportMonthsByStudentId(@Param("tenantId") Long tenantId);

    /** Élèves ayant réglé une facture transport dont l'échéance liée tombe dans [from, to] (un mois
     * calendaire) — §retour comptable : "qui a payé le mois en cours", filtre par mois sur la liste
     * des abonnés. L'échéance (pas la date de paiement) détermine le mois couvert, cohérent avec le
     * reste de la facturation (voir PaymentReminderDataPort côté communication, même logique). */
    @Query("SELECT DISTINCT i.studentId FROM TransportInvoiceMirrorEntity i "
            + "JOIN TransportInstallmentMirrorEntity inst ON i.installmentId = inst.id "
            + "WHERE i.tenantId = :tenantId AND i.paidAmount > 0 AND inst.dueDate BETWEEN :from AND :to "
            + "AND i.feeId IN (SELECT f.id FROM TransportSchoolFeeMirrorEntity f WHERE f.tenantId = :tenantId AND f.type = 'TRANSPORT')")
    List<Long> findStudentIdsPaidForMonth(@Param("tenantId") Long tenantId,
                                          @Param("from") LocalDate from, @Param("to") LocalDate to);
}
