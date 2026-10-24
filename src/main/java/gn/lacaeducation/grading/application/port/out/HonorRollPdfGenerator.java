package gn.lacaeducation.grading.application.port.out;

import java.math.BigDecimal;
import java.util.List;

/** Renders the school-wide honour roll / ranking (palmarès, tableau d'honneur) to PDF bytes — best
 * students of the WHOLE establishment for a period, across all classes (not a single class). */
public interface HonorRollPdfGenerator {

    byte[] generate(HonorRoll roll);

    /** Self-contained, human-readable data for the ranking document (names resolved upstream). */
    record HonorRoll(
            Long tenantId,
            String periodLabel,
            String schoolYear,
            List<Row> rows) {}

    /** One ranked student line. {@code className} is per-row (not a single roll-wide class) since
     * laureates come from different classes. {@code exAequo} : moyenne générale strictement égale à
     * au moins un autre élève du même rang (classement standard "1,1,3,4,4…" — jamais "1,2,3" en cas
     * d'égalité, reproduit fidèlement la maquette de référence "Tableau d'honneur.dc.html"). */
    record Row(Integer rank, boolean exAequo, String studentName, String studentFirstName, String studentLastName,
              BigDecimal average, String honor, String studentPhoto, String className) {}
}
