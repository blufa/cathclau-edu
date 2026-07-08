package gn.lacaeducation.grading.application.port.in;

import java.math.BigDecimal;
import java.util.List;

/**
 * Simulateur de bulletins (§9) : calcule à blanc les moyennes et rangs d'une classe/période et
 * détecte les anomalies (élèves sans notes, matières incomplètes) avant publication. Aucune écriture.
 */
public interface GradeSimulationUseCase {

    record StudentSimulation(Long studentId, String studentName, BigDecimal overallAverage, Integer rank,
                             int gradedSubjects, List<Long> missingSubjects, boolean hasNoGrades) {}

    record SimulationReport(Long classId, Long periodId, int studentCount,
                            int studentsWithoutGrades, int studentsWithMissingSubjects,
                            List<StudentSimulation> students) {}

    SimulationReport simulate(Long classId, Long periodId);
}
