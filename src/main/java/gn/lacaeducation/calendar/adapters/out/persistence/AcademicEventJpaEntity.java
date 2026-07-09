package gn.lacaeducation.calendar.adapters.out.persistence;

import gn.lacaeducation.calendar.domain.AcademicEventType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "academic_events")
@Getter
@Setter
@org.hibernate.annotations.Filter(name = "tenantFilter")
public class AcademicEventJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(name = "academic_year_id", nullable = false) private Long academicYearId;
    @Column(nullable = false) private String title;
    @Enumerated(EnumType.STRING) @Column(name = "event_type", nullable = false, length = 24) private AcademicEventType eventType;
    @Column(name = "start_date", nullable = false) private LocalDate startDate;
    @Column(name = "end_date") private LocalDate endDate;
    @Column(columnDefinition = "text") private String description;
    @Column(name = "created_at") private LocalDateTime createdAt;
}
