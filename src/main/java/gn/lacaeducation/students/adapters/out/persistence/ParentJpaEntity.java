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
}
