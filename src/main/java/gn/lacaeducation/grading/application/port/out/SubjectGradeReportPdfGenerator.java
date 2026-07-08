package gn.lacaeducation.grading.application.port.out;

import gn.lacaeducation.grading.application.port.in.GradeUseCase.SubjectGradeReport;

/** Renders a per-subject grade report (relevé de notes) to PDF bytes. */
public interface SubjectGradeReportPdfGenerator {
    byte[] generate(SubjectGradeReport report);
}
