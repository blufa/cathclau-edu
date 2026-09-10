package gn.lacaeducation.attendance.adapters.out.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

@Entity
@Table(name = "attendance_settings")
@Getter @Setter @NoArgsConstructor
class AttendanceSettingsJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false, unique = true) private Long tenantId;
    @Column(name = "transition_margin_minutes", nullable = false) private Integer transitionMarginMinutes;
    @Column(name = "student_transition_margin_minutes", nullable = false) private Integer studentTransitionMarginMinutes;
    @Column(name = "employee_check_in_time", nullable = false) private LocalTime employeeCheckInTime;
    @Column(name = "employee_check_out_time", nullable = false) private LocalTime employeeCheckOutTime;
}
