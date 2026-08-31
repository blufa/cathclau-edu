package gn.lacaeducation.academics.adapters.out.persistence;

import gn.lacaeducation.academics.domain.valueobject.JourSemaine;
import gn.lacaeducation.academics.domain.valueobject.ScheduleExceptionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "schedule_exceptions")
@Getter @Setter @NoArgsConstructor
@org.hibernate.annotations.Filter(name = "tenantFilter")
public class ScheduleExceptionJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(name = "original_schedule_id", nullable = false) private Long originalScheduleId;
    @Column(name = "week_start_date", nullable = false) private LocalDate weekStartDate;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private ScheduleExceptionType type;
    @Enumerated(EnumType.STRING) @Column(name = "new_day") private JourSemaine newDay;
    @Column(name = "new_start_time") private LocalTime newStartTime;
    @Column(name = "new_end_time") private LocalTime newEndTime;
    @Column(name = "new_room_id") private Long newRoomId;
    @Column(name = "new_teacher_id") private Long newTeacherId;
    private String reason;
}
