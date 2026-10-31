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

    /** Élèves ayant réglé une facture transport dont l'échéance liée COUVRE le mois demandé (format
     * "YYYY-MM") — §retour comptable : "qui a payé le mois en cours", filtre par mois sur la liste des
     * abonnés. Matché sur {@code inst.months} (le mois réellement couvert), jamais {@code dueDate}
     * (simple échéance de paiement, le 5 du mois SUIVANT par convention — matcher dessus décalait tout
     * le filtre d'un mois, §retour utilisateur : "si tu filtres par mois rien ne vient"). LIKE : une
     * tranche multi-mois stocke plusieurs mois séparés par des virgules (ex. "2026-05,2026-06"). */
    @Query("SELECT DISTINCT i.studentId FROM TransportInvoiceMirrorEntity i "
            + "JOIN TransportInstallmentMirrorEntity inst ON i.installmentId = inst.id "
            + "WHERE i.tenantId = :tenantId AND i.paidAmount > 0 AND inst.months LIKE CONCAT('%', :month, '%') "
            + "AND i.feeId IN (SELECT f.id FROM TransportSchoolFeeMirrorEntity f WHERE f.tenantId = :tenantId AND f.type = 'TRANSPORT')")
    List<Long> findStudentIdsPaidForMonth(@Param("tenantId") Long tenantId, @Param("month") String month);
}
