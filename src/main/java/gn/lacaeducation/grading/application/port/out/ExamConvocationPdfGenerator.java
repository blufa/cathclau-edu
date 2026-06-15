package gn.lacaeducation.grading.application.port.out;

import gn.lacaeducation.grading.domain.Exam;

/** Renders an exam convocation to PDF bytes. */
public interface ExamConvocationPdfGenerator {
    byte[] generate(Exam exam);
}
