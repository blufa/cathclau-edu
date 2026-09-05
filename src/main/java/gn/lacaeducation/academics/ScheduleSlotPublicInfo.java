package gn.lacaeducation.academics;

/** Public view of a timetable slot exposed to other modules (e.g. a teacher's consolidated schedule). */
public record ScheduleSlotPublicInfo(Long id, Long classId, Long subjectId, Long teacherId,
                                     Long roomId, String day, String startTime, String endTime, String label) {}
