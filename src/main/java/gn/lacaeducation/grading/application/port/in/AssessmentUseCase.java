package gn.lacaeducation.grading.application.port.in;
import gn.lacaeducation.grading.domain.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
public interface AssessmentUseCase {
    /** @param examId composition (§Exam) qui fait naître cette évaluation — {@code null} pour une
     * création normale (Nouvelle évaluation) ; voir {@code ExamService#gradeEntryAssessment}. */
    record CreateAssessmentCommand(Long tenantId, Long classId, Long subjectId, Long teacherId, Long periodId,
                                    AssessmentType type, Long assessmentTypeId, String label, LocalDate date,
                                    BigDecimal coefficient, BigDecimal maxScore, Long examId) {
        public CreateAssessmentCommand(Long tenantId, Long classId, Long subjectId, Long teacherId, Long periodId,
                                        AssessmentType type, Long assessmentTypeId, String label, LocalDate date,
                                        BigDecimal coefficient, BigDecimal maxScore) {
            this(tenantId, classId, subjectId, teacherId, periodId, type, assessmentTypeId, label, date,
                    coefficient, maxScore, null);
        }
    }
    Assessment create(CreateAssessmentCommand command);
    Assessment update(Long id, CreateAssessmentCommand command);
    Optional<Assessment> findById(Long id);
    List<Assessment> findByClass(Long classId);
    List<Assessment> findByPeriod(Long periodId);
    /** Supprime l'évaluation ET ses notes déjà saisies le cas échéant (§retour utilisateur : "il faut
     * aussi me donner la possibilité de supprimer une évaluation même si les notes sont saisies") —
     * jamais bloqué par la contrainte de clé étrangère grades→assessments. */
    void delete(Long id);
    /** Fiche de notes vierge (PDF) à imprimer et donner à l'enseignant : liste des élèves de la classe. */
    byte[] planPdf(Long classId, Long subjectId, Long academicYearId);
    /** PDF téléchargeable d'UNE évaluation précise, avec les notes déjà saisies le cas échéant. */
    byte[] assessmentGradeSheetPdf(Long assessmentId);

    /** Un groupe d'interrogations créées séparément (avant la double saisie Orale/Écrite) pour la
     * même classe/matière/période/date — signe de l'ancien flux "deux évaluations au lieu d'une"
     * (§retour censeur). Candidat à une fusion manuelle via {@link #mergeDuplicates}, jamais fusionné
     * automatiquement. */
    record DuplicateGroup(Long classId, Long subjectId, Long periodId, LocalDate date, List<Assessment> assessments) {}

    /** Scanne tout le tenant courant à la recherche de groupes d'interrogations (type QUIZ) partageant
     * classe/matière/période/date — lecture seule, ne modifie rien. */
    List<DuplicateGroup> findDuplicateQuizGroups();

    /** Fusionne deux évaluations en double (même classe/matière/période) : pour chaque élève, la note
     * de {@code keepAssessmentId} devient sa case A et celle de {@code removeAssessmentId} sa case B
     * (moyenne recalculée côté serveur, voir {@code GradeService}) ; {@code removeAssessmentId} et ses
     * notes sont ensuite supprimées. Un élève noté sur un seul des deux ne perd pas sa note (reportée
     * dans la case correspondante). Réservé SCHOOL_ADMIN/DIRECTOR. */
    Assessment mergeDuplicates(Long keepAssessmentId, Long removeAssessmentId);
}
