package gn.lacaeducation.attendance.adapters.out.persistence;
import gn.lacaeducation.attendance.domain.AttendanceStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalTime;
@Entity @Table(name = "student_attendances")
@Getter @Setter @NoArgsConstructor
@org.hibernate.annotations.Filter(name = "tenantFilter")
class StudentAttendanceJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(name = "student_id", nullable = false) private Long studentId;
    @Column(name = "class_id", nullable = false) private Long classId;
    private LocalDate date;
    @Enumerated(EnumType.STRING) private AttendanceStatus status;
    private String reason;
    @Column(name = "parent_notified") private Boolean parentNotified;
    private Boolean justified;
    @Column(name = "justification_doc", length = 1024) private String justificationDoc;
    @Column(name = "check_in_time") private LocalTime checkInTime;
    @Column(name = "late_minutes") private Integer lateMinutes;
}
