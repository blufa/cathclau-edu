package gn.lacaeducation.grading;

import java.math.BigDecimal;
import java.util.Optional;

/** API publique du module grading : bulletins publiés d'un élève (pour les documents). */
public interface StudentReportProvider {
    record ReportInfo(BigDecimal average, Integer rank, Integer classSize, String mention, String decision) {}

    Optional<ReportInfo> latestPublished(Long studentId);

    /** Un bulletin publié téléchargeable. */
    record PublishedReportInfo(Long reportCardId, Long periodId, BigDecimal average, String mention) {}

    /** Tous les bulletins publiés d'un élève (portail parent §11 — documents). */
    java.util.List<PublishedReportInfo> listPublished(Long studentId);
}
