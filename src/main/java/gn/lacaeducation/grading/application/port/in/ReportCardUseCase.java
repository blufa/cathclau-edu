package gn.lacaeducation.grading.application.port.in;
import gn.lacaeducation.grading.domain.*;
import java.util.List;
import java.util.Optional;
public interface ReportCardUseCase {
    record GenerateReportCardCommand(Long studentId, Long classId, Long periodId) {}
    record GenerateBulkCommand(Long classId, Long periodId) {}
    ReportCard generate(GenerateReportCardCommand command);
    List<ReportCard> generateBulk(GenerateBulkCommand command);
    ReportCard publish(Long id);
    /** Ajuste manuellement l'appréciation du conseil de classe (override du calcul automatique). */
    ReportCard setDistinction(Long id, Distinction distinction);
    Optional<ReportCard> findById(Long id);
    List<ReportCard> findByClass(Long classId, Long periodId);
    Optional<ReportCard> findByStudent(Long studentId, Long periodId);
    List<ReportCard> findByStudentId(Long studentId);
    List<ReportCardLine> findLines(Long reportCardId);
    Optional<String> getPdf(Long id);
    /** Palmarès / tableau d'honneur de l'établissement pour une période (PDF) : les meilleurs élèves
     * TOUTES CLASSES CONFONDUES, classés par moyenne générale décroissante — pas un classement par
     * classe (§retour fondateur : "ce n'est pas pour une classe, c'est pour les meilleurs élèves de
     * l'école, parmi toutes les classes"). Portée = tenant courant (TenantContext). */
    byte[] honorRollPdf(Long periodId);
    /** Centralisateur de classe (PDF) : une ligne par élève, moyennes générales mensuelles par
     * semestre puis bilan annuel (moyenne, rang, mention, décision) — portée = année scolaire entière
     * (les deux semestres), pas une seule période. */
    byte[] classCentralizerPdf(Long classId, Long academicYearId);
    /** Génère et renvoie les octets du PDF du bulletin (pour affichage direct, sans Cloudinary). */
    byte[] pdfBytes(Long id);
    void generatePdfAsync(Long id);
    void delete(Long id);
}
