package gn.lacaeducation.calendar.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Un événement du calendrier académique (férié, vacances, composition, réunion, échéance…). */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcademicEvent {
    private Long id;
    private Long tenantId;
    private Long academicYearId;
    private String title;
    private AcademicEventType eventType;
    private LocalDate startDate;
    private LocalDate endDate;
    private String description;
    private LocalDateTime createdAt;
}
