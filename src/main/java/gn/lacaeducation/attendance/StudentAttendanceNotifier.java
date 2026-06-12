package gn.lacaeducation.attendance;

/**
 * Public API of the attendance module letting the communication side mark a student attendance record
 * as parent-notified after the absence notification has been sent.
 */
public interface StudentAttendanceNotifier {
    void markParentNotified(Long attendanceId);
}
