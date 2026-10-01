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
    /** Lien direct vers une fiche Teacher/Employee, SANS compte de connexion requis — pour le
     *  personnel qui ne veut/ne peut pas avoir de compte (ex. pas de smartphone Android). Alternative à
     *  la détection par {@code userId} partagé (voir {@code StaffChildBenefitService#isStaffChild}). */
    private StaffLinkType linkedStaffType;
    private Long linkedStaffId;
}
