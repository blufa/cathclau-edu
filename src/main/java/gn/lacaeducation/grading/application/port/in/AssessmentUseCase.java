package gn.lacaeducation.grading.application.port.in;
import gn.lacaeducation.grading.domain.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
public interface AssessmentUseCase {
    record CreateAssessmentCommand(Long tenantId, Long classId, Long subjectId, Long teacherId, Long periodId, AssessmentType type, Long assessmentTypeId, String label, LocalDate date, BigDecimal coefficient, BigDecimal maxScore) {}
    Assessment create(CreateAssessmentCommand command);
    Assessment update(Long id, CreateAssessmentCommand command);
    Optional<Assessment> findById(Long id);
    List<Assessment> findByClass(Long classId);
    List<Assessment> findByPeriod(Long periodId);
    void delete(Long id);
    /** Fiche de notes vierge (PDF) à imprimer et donner à l'enseignant : liste des élèves de la classe. */
    byte[] planPdf(Long classId, Long subjectId, Long academicYearId);
    /** PDF téléchargeable d'UNE évaluation précise, avec les notes déjà saisies le cas échéant. */
    byte[] assessmentGradeSheetPdf(Long assessmentId);
}
