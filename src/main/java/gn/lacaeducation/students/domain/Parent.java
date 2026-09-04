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
    private String photo;
    /** Rempli uniquement dans le contexte d'un élève : responsable principal (« responsable 1 ») ? */
    private boolean primaryGuardian;
    /** Clé d'idempotence fournie par le client (mode hors-ligne) : rejouer avec la même clé après une
     *  coupure réseau renvoie ce parent au lieu d'en créer un second. Null pour une création en ligne. */
    private String idempotencyKey;
}
