package gn.lacaeducation.academics.adapters.out.persistence;

import gn.lacaeducation.academics.domain.valueobject.JourSemaine;
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

import java.time.LocalTime;

@Entity
@Table(name = "schedules")
@Getter @Setter @NoArgsConstructor
@org.hibernate.annotations.Filter(name = "tenantFilter")
public class EmploiDuTempsJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(name = "class_id", nullable = false) private Long schoolClassId;
    @Column(name = "subject_id") private Long subjectId;
    @Column(name = "teacher_id") private Long teacherId;
    @Column(name = "room_id") private Long roomId;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private JourSemaine day;
    @Column(name = "start_time") private LocalTime startTime;
    @Column(name = "end_time") private LocalTime endTime;
}
