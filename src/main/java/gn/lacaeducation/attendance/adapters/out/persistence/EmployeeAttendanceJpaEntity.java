package gn.lacaeducation.attendance.adapters.out.persistence;
import gn.lacaeducation.attendance.domain.EmployeeAttendanceStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
@Entity @Table(name = "employee_attendances")
@Getter @Setter @NoArgsConstructor
@org.hibernate.annotations.Filter(name = "tenantFilter")
class EmployeeAttendanceJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(name = "employee_id", nullable = false) private Long employeeId;
    private LocalDate date;
    @Enumerated(EnumType.STRING) private EmployeeAttendanceStatus status;
    private String reason;
    @Column(name = "check_in_time") private LocalTime checkInTime;
    @Column(name = "check_out_time") private LocalTime checkOutTime;
    @Column(name = "late_minutes") private Integer lateMinutes;
    @Column(name = "early_minutes") private Integer earlyMinutes;
    @Column(name = "created_at") private LocalDateTime createdAt;
}
