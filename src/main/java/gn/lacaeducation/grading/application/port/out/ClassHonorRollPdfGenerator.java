package gn.lacaeducation.grading.application.port.out;

import java.math.BigDecimal;
import java.util.List;

/** Renders a class honour roll / ranking (palmarès, tableau d'honneur) to PDF bytes. */
public interface ClassHonorRollPdfGenerator {

    byte[] generate(HonorRoll roll);

    /** Self-contained, human-readable data for the ranking document (names resolved upstream). */
    record HonorRoll(
            Long tenantId,
            String className,
            String periodLabel,
            String schoolYear,
            BigDecimal classAverage,
            List<Row> rows) {}

    /** One ranked student line. */
    record Row(Integer rank, String studentName, BigDecimal average, String honor) {}
}
