package gn.lacaeducation.grading.adapters.out.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name = "exam_supervisions") @Getter @Setter
@org.hibernate.annotations.Filter(name = "tenantFilter")
public class ExamSupervisionJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(name = "exam_id", nullable = false) private Long examId;
    @Column(name = "room_id", nullable = false) private Long roomId;
    @Column(name = "teacher_id", nullable = false) private Long teacherId;
}
