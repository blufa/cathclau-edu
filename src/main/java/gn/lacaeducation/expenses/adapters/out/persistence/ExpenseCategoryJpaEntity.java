package gn.lacaeducation.expenses.adapters.out.persistence;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
@Entity @Table(name = "expense_category") @Getter @Setter
@org.hibernate.annotations.Filter(name = "tenantFilter")
public class ExpenseCategoryJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(nullable = false) private String name;
    @Column(name = "parent_id") private Long parentId;
}
