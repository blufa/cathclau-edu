package gn.lacaeducation.academics;
import java.time.LocalDate;
public record PeriodPublicInfo(Long id, String label, String type, LocalDate startDate, LocalDate endDate,
                               /** Période "composition" associée (§retour fondateur) — voir
                                * {@code gn.lacaeducation.academics.domain.Periode#getCompositionPeriodId()}. */
                               Long compositionPeriodId,
                               /** Année scolaire de la période — pour retrouver les autres périodes de la
                                * même année (ex. le 1er semestre quand on affiche le bulletin du 2ème,
                                * §retour fondateur). */
                               Long academicYearId) {}
