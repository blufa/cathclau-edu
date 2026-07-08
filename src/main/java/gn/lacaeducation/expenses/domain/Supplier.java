package gn.lacaeducation.expenses.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A supplier/vendor an expense can be attached to. */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Supplier {
    private Long id;
    private Long tenantId;
    private String name;
    private String type;
    private String phone;
    private String email;
    private String address;
    private String bankAccount;
}
