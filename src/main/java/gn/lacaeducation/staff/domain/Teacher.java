package gn.lacaeducation.staff.domain;

import lombok.*;
import java.time.LocalDate;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Teacher {
    private Long id;
    private Long tenantId;
    private Long userId;
    private String staffNumber;
    private String firstName;
    private String lastName;
    private String phone;
    private String email;
    private String specialization;
    private LocalDate hireDate;
}
