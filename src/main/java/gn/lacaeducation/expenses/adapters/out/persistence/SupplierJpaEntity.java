package gn.lacaeducation.expenses.adapters.out.persistence;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
@Entity @Table(name = "supplier") @Getter @Setter
@org.hibernate.annotations.Filter(name = "tenantFilter")
public class SupplierJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(nullable = false) private String name;
    private String type;
    private String phone;
    private String email;
    private String address;
    @Column(name = "bank_account") private String bankAccount;
}
