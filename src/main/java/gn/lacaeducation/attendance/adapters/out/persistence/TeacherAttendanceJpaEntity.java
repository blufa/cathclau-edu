package gn.lacaeducation.attendance.adapters.out.persistence;
import gn.lacaeducation.attendance.domain.TeacherAttendanceStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;
@Entity @Table(name = "teacher_attendances")
@Getter @Setter @NoArgsConstructor
class TeacherAttendanceJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "teacher_id", nullable = false) private Long teacherId;
    private LocalDate date;
    @Enumerated(EnumType.STRING) private TeacherAttendanceStatus status;
    private String reason;
}
