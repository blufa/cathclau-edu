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
    /** Palmarès / tableau d'honneur d'une classe pour une période (PDF, élèves classés par rang). */
    byte[] classHonorRollPdf(Long classId, Long periodId);
    /** Génère et renvoie les octets du PDF du bulletin (pour affichage direct, sans Cloudinary). */
    byte[] pdfBytes(Long id);
    void generatePdfAsync(Long id);
    void delete(Long id);
}
