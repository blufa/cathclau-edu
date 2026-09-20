package gn.lacaeducation.attendance.application.port.out;

import java.time.LocalDate;
import java.util.List;

/** Renders attendance documents (absentee list, lateness report) to PDF bytes. */
public interface AttendanceReportPdfGenerator {

    byte[] absenteeList(AbsenteeList data);

    byte[] latenessReport(LatenessReport data);

    /** Liste des absents d'une classe pour une date donnée (noms résolus en amont). */
    record AbsenteeList(Long tenantId, String className, LocalDate date, List<AbsenteeRow> rows) {}

    record AbsenteeRow(String studentName, String status, boolean justified, String reason) {}

    /** Rapport des retards d'une classe sur une période (agrégé par élève). */
    record LatenessReport(Long tenantId, String className, LocalDate from, LocalDate to, List<LatenessRow> rows) {}

    record LatenessRow(String studentName, int count, String dates) {}

    byte[] teacherLatenessReport(TeacherLatenessReport data);

    /** Une ligne chronologique du récapitulatif enseignant : soit un écart de ponctualité sur une
     * séance précise (matière/classe renseignées, retard et/ou départ anticipé &gt; 0), soit un jour
     * ABSENT (matière/classe/minutes non applicables, {@code absenceReason} renseigné). */
    record TeacherReportRow(LocalDate date, String subjectLabel, String className,
                            int lateMinutes, int earlyMinutes, boolean absent, String absenceReason) {}

    /** Récapitulatif retards/absences d'un enseignant sur une période — remis en cas de contestation
     * de salaire (justifie chaque minute déduite, matière et classe par matière et par classe). */
    record TeacherLatenessReport(Long tenantId, String teacherName, LocalDate from, LocalDate to,
                                 List<TeacherReportRow> rows) {}
}
