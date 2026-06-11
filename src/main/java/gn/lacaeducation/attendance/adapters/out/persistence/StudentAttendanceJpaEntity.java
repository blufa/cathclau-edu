package gn.lacaeducation.attendance.adapters.out.persistence;
import gn.lacaeducation.attendance.domain.AttendanceStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;
@Entity @Table(name = "student_attendances")
@Getter @Setter @NoArgsConstructor
class StudentAttendanceJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "student_id", nullable = false) private Long studentId;
    @Column(name = "class_id", nullable = false) private Long classId;
    private LocalDate date;
    @Enumerated(EnumType.STRING) private AttendanceStatus status;
    private String reason;
    @Column(name = "parent_notified") private Boolean parentNotified;
}
