package gn.lacaeducation.grading.application.port.out;

import gn.lacaeducation.grading.domain.ReportCard;
import gn.lacaeducation.grading.domain.ReportCardLine;
import java.util.List;

/** Renders a report card (header + per-subject lines + summary) to PDF bytes. */
public interface ReportCardPdfGenerator {
    byte[] generate(ReportCard reportCard, List<ReportCardLine> lines);
}
