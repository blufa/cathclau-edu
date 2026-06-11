package gn.lacaeducation.students.domain;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Student {
    private Long id;
    private Long tenantId;
    private Long userId;
    private String studentNumber;
    private String photo;
    private String firstName;
    private String lastName;
    private Gender gender;
    private LocalDate birthDate;
    private String birthPlace;
    private String address;
    private String phone;
    private String email;
    private String nationality;
    private LocalDateTime createdAt;
}
