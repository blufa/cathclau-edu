package gn.lacaeducation.grading.application.port.out;
import gn.lacaeducation.grading.domain.Assessment;
import gn.lacaeducation.grading.domain.AssessmentType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
public interface AssessmentRepository {
    Assessment save(Assessment assessment);
    Optional<Assessment> findById(Long id);
    List<Assessment> findByClassId(Long classId);
    List<Assessment> findByPeriodId(Long periodId);
    List<Assessment> findByClassIdAndPeriodId(Long classId, Long periodId);
    List<Assessment> findByExamId(Long examId);
    void deleteById(Long id);
    /** Même classe/matière/période/type/libellé/date qu'une évaluation déjà existante — signe d'un
     * doublon accidentel (double clic, nouvelle tentative après un envoi qui semblait échoué...).
     * Ignore volontairement l'id courant pour permettre une modification sans se bloquer elle-même. */
    boolean existsDuplicate(Long classId, Long subjectId, Long periodId, AssessmentType type,
                             String label, LocalDate date, Long excludingId);
}
