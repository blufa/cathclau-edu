package gn.lacaeducation.academics;

import java.time.LocalDate;
import java.time.LocalTime;

/** Public view of a school event (kermesse, match, entraînement...) exposed to other modules — notably
 * payroll, to keep teachers paid on hours covered by such an event even though the class didn't happen.
 * {@code startTime}/{@code endTime} null = covers the whole day. */
public record SchoolEventPublicInfo(Long id, String label, LocalDate startDate, LocalDate endDate,
                                    LocalTime startTime, LocalTime endTime) {}
