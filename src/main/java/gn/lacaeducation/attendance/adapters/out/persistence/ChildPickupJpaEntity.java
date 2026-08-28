package gn.lacaeducation.attendance.adapters.out.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity @Table(name = "child_pickups")
@Getter @Setter @NoArgsConstructor
class ChildPickupJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(name = "student_id", nullable = false) private Long studentId;
    @Column(name = "class_id", nullable = false) private Long classId;
    @Column(name = "pickup_date", nullable = false) private LocalDate pickupDate;
    @Column(name = "pickup_time", nullable = false) private LocalTime pickupTime;
    @Column(name = "picker_parent_id") private Long pickerParentId;
    @Column(name = "picker_name", nullable = false) private String pickerName;
    @Column(name = "picker_relation") private String pickerRelation;
    @Column(name = "created_at") private LocalDateTime createdAt;
}
