package gn.lacaeducation.students.adapters.out.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity @Table(name = "parents")
@Getter @Setter @NoArgsConstructor
@org.hibernate.annotations.Filter(name = "tenantFilter")
class ParentJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(name = "user_id") private Long userId;
    @Column(name = "first_name") private String firstName;
    @Column(name = "last_name") private String lastName;
    private String phone;
    private String email;
    private String occupation;
    private String address;
    private String photo;
    @Column(name = "idempotency_key") private String idempotencyKey;
    @Enumerated(EnumType.STRING) @Column(name = "linked_staff_type")
    private gn.lacaeducation.students.domain.StaffLinkType linkedStaffType;
    @Column(name = "linked_staff_id") private Long linkedStaffId;
    @Column(name = "exclude_from_auto_deduction", nullable = false) private boolean excludeFromAutoDeduction;
}
