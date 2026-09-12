package gn.lacaeducation.payroll.adapters.out.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name = "teacher_vacation_pay_months")
@Getter @Setter
@org.hibernate.annotations.Filter(name = "tenantFilter")
public class TeacherVacationPayMonthJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(name = "teacher_id", nullable = false) private Long teacherId;
    @Column(nullable = false) private int year;
    @Column(nullable = false) private int month;
}
