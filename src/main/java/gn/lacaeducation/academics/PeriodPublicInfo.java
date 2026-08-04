package gn.lacaeducation.academics;
import java.time.LocalDate;
public record PeriodPublicInfo(Long id, String label, String type, LocalDate startDate, LocalDate endDate) {}
