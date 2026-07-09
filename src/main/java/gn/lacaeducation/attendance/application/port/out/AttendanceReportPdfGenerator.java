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
}
