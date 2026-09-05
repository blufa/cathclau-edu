package gn.lacaeducation.attendance.adapters.out.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "attendance_settings")
@Getter @Setter @NoArgsConstructor
class AttendanceSettingsJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false, unique = true) private Long tenantId;
    @Column(name = "transition_margin_minutes", nullable = false) private Integer transitionMarginMinutes;
}
