package gn.lacaeducation.grading.application.port.in;

import gn.lacaeducation.grading.domain.AnnualReportCard;

import java.util.List;
import java.util.Optional;

/** Bulletin annuel (agrégation à la volée des deux bulletins semestriels, §documents ministère). */
public interface AnnualReportCardUseCase {

    /** Bulletins annuels de tous les élèves ayant un bulletin semestriel dans la classe, classés par rang. */
    List<AnnualReportCard> findByClass(Long tenantId, Long classId, Long academicYearId);

    Optional<AnnualReportCard> findByStudent(Long tenantId, Long studentId, Long classId, Long academicYearId);

    /** PDF du bulletin annuel officiel d'un élève. */
    byte[] pdfBytes(Long tenantId, Long studentId, Long classId, Long academicYearId);
}
