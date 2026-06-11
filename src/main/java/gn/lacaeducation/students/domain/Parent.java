package gn.lacaeducation.students.domain;

import lombok.*;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Parent {
    private Long id;
    private Long tenantId;
    private Long userId;
    private String firstName;
    private String lastName;
    private String phone;
    private String email;
    private String occupation;
    private String address;
}
