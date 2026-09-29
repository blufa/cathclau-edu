package gn.lacaeducation.academics;

import java.time.LocalDate;
import java.time.LocalTime;

/** Public view of a school event (kermesse, match, entraînement...) exposed to other modules — notably
 * payroll, which only keeps a teacher paid on hours covered by such an event if the direction
 * explicitly credited them for it (see {@code AcademicsDataProvider#isTeacherCreditedForSchoolEvent}).
 * {@code startTime}/{@code endTime} null = covers the whole day. A public holiday (jour férié) is a
 * SEPARATE concept from the academic calendar module ({@code AcademicEventType.HOLIDAY}), always paid
 * automatically — never modeled as a {@code SchoolEvent}. */
public record SchoolEventPublicInfo(Long id, String label, LocalDate startDate, LocalDate endDate,
                                    LocalTime startTime, LocalTime endTime) {}
