package gn.lacaeducation.academics.adapters.out.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

@Entity
@Table(name = "primary_timetable_settings")
@Getter @Setter @NoArgsConstructor
class PrimaryTimetableSettingsJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false, unique = true) private Long tenantId;
    @Column(name = "grid_start_time", nullable = false) private LocalTime gridStartTime;
    @Column(name = "grid_end_time", nullable = false) private LocalTime gridEndTime;
    @Column(name = "band_minutes", nullable = false) private Integer bandMinutes;
}
