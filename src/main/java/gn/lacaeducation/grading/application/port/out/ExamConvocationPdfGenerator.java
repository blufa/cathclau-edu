package gn.lacaeducation.grading.application.port.out;

import gn.lacaeducation.grading.domain.Exam;

/** Renders an exam convocation to PDF bytes (branded header + names + footer). */
public interface ExamConvocationPdfGenerator {

    byte[] generate(Exam exam, ExamConvocationContext context);

    /** Human-readable names resolved from other modules so the convocation shows names, not raw IDs. */
    record ExamConvocationContext(String className, String periodLabel, String schoolYear) {
        public static ExamConvocationContext empty() {
            return new ExamConvocationContext(null, null, null);
        }
    }
}
