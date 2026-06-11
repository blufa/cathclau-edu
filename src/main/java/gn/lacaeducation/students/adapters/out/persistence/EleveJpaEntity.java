package gn.lacaeducation.students.adapters.out.persistence;

import gn.lacaeducation.students.domain.Gender;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity @Table(name = "students")
@Getter @Setter @NoArgsConstructor
class EleveJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(name = "user_id") private Long userId;
    @Column(name = "student_number") private String studentNumber;
    private String photo;
    @Column(name = "first_name") private String firstName;
    @Column(name = "last_name") private String lastName;
    @Enumerated(EnumType.STRING) private Gender gender;
    @Column(name = "birth_date") private LocalDate birthDate;
    @Column(name = "birth_place") private String birthPlace;
    private String address;
    private String phone;
    private String email;
    private String nationality;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
}
