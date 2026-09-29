package gn.lacaeducation.academics.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Enseignant explicitement crédité (payé) par la direction pour un {@link SchoolEventJpaEntity} de
 *  type OTHER — voir {@code SchoolEventType}. */
@Entity
@Table(name = "school_event_credited_teachers")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class SchoolEventCreditedTeacherJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "school_event_id", nullable = false) private Long schoolEventId;
    @Column(name = "teacher_id", nullable = false) private Long teacherId;
}
