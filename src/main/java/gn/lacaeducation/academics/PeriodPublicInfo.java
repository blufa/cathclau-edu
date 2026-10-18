package gn.lacaeducation.academics;
import java.time.LocalDate;
public record PeriodPublicInfo(Long id, String label, String type, LocalDate startDate, LocalDate endDate,
                               /** Période "composition" associée (§retour fondateur) — voir
                                * {@code gn.lacaeducation.academics.domain.Periode#getCompositionPeriodId()}. */
                               Long compositionPeriodId) {}
