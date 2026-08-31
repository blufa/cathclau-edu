package gn.lacaeducation.attendance.adapters.out.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity @Table(name = "student_checkins")
@Getter @Setter @NoArgsConstructor
class StudentCheckinJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(name = "student_id", nullable = false) private Long studentId;
    @Column(name = "class_id", nullable = false) private Long classId;
    @Column(name = "checkin_date", nullable = false) private LocalDate checkinDate;
    @Column(name = "schedule_slot_id") private Long scheduleSlotId;
    @Column(name = "session_start") private LocalTime sessionStart;
    @Column(name = "session_end") private LocalTime sessionEnd;
    @Column(name = "check_in_time", nullable = false) private LocalTime checkInTime;
    @Column(name = "check_out_time") private LocalTime checkOutTime;
    @Column(name = "late_minutes", nullable = false) private Integer lateMinutes;
    @Column(name = "created_at") private LocalDateTime createdAt;
}
