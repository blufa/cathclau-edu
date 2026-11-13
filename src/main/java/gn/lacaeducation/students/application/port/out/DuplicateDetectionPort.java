package gn.lacaeducation.students.application.port.out;

import java.math.BigDecimal;
import java.util.List;

/** Candidats doublons élèves (prénom/nom élève + nom de famille d'un parent, normalisés, identiques —
 * plus limité à la même classe) avec leur profil financier réel — voir
 * {@code DuplicateStudentJpaRepository} pour le détail de la requête. */
public interface DuplicateDetectionPort {
    record StudentFinancialRow(Long studentId, String firstName, String lastName, String photo,
                               Long classId, String className, String parentLastName,
                               BigDecimal totalInvoiced, BigDecimal totalPaid, long monthlyInvoiceCount) {}

    List<StudentFinancialRow> findDuplicateCandidates(Long tenantId);
}
