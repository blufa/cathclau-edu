package gn.lacaeducation.academics.domain;

import gn.lacaeducation.academics.domain.valueobject.JourSemaine;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmploiDuTemps {
    private Long id;
    private Long tenantId;
    private Long schoolClassId;
    private Long subjectId;
    private Long teacherId;
    private Long roomId;
    /** Intitulé libre affiché quand il n'y a pas de matière formelle (heure creuse : étude, permanence, TD...). */
    private String label;
    private JourSemaine day;
    private LocalTime startTime;
    private LocalTime endTime;
}
